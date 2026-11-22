package com.grash.service;

import com.grash.dto.analytics.AnalyticsFilter;
import com.grash.dto.analytics.assets.AssetAvailability;
import com.grash.dto.analytics.workOrders.MaintenanceKpis;
import com.grash.dto.analytics.workOrders.MaintenanceKpisByDate;
import com.grash.dto.analytics.workOrders.PriorityResponseTimes;
import com.grash.exception.CustomException;
import com.grash.model.PreventiveMaintenance;
import com.grash.model.Request;
import com.grash.model.WorkOrder;
import com.grash.model.enums.AssetStatus;
import com.grash.model.enums.Priority;
import com.grash.model.enums.Status;
import com.grash.repository.AssetDowntimeRepository;
import com.grash.repository.WorkOrderRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaintenanceKpiServiceTest {

    private static final long HOUR = 3_600_000L;
    private static final long DAY = 24 * HOUR;
    private static final Date T0 = new Date(1_000 * DAY);

    @Mock
    private WorkOrderRepository workOrderRepository;
    @Mock
    private AssetDowntimeRepository assetDowntimeRepository;

    @InjectMocks
    private MaintenanceKpiService maintenanceKpiService;

    @Nested
    class OpenAt {

        @Test
        void openWorkOrder_isOpenFromItsCreation() {
            WorkOrder open = workOrder(0, Status.OPEN);

            assertFalse(MaintenanceKpiService.isOpenAt(open, at(-1)));
            assertTrue(MaintenanceKpiService.isOpenAt(open, at(0)));
            assertTrue(MaintenanceKpiService.isOpenAt(open, at(10)));
        }

        @Test
        void completedWorkOrder_isOpenUntilItsCompletion() {
            WorkOrder completed = completed(workOrder(0, Status.COMPLETE), 5);

            assertTrue(MaintenanceKpiService.isOpenAt(completed, at(4)));
            assertFalse(MaintenanceKpiService.isOpenAt(completed, at(5)));
            assertFalse(MaintenanceKpiService.isOpenAt(completed, at(6)));
        }

        @Test
        void completedWithoutDate_isNeverOpen() {
            assertFalse(MaintenanceKpiService.isOpenAt(workOrder(0, Status.COMPLETE), at(1)));
        }

        @Test
        void reopenedWorkOrder_isOpenDespiteAnOldCompletionDate() {
            WorkOrder reopened = completed(workOrder(0, Status.IN_PROGRESS), 2);

            assertTrue(MaintenanceKpiService.isOpenAt(reopened, at(3)));
        }

        @Test
        void archivedWorkOrder_isNotPartOfTheBacklog() {
            WorkOrder archived = workOrder(0, Status.OPEN);
            archived.setArchived(true);

            assertFalse(MaintenanceKpiService.isOpenAt(archived, at(1)));
        }
    }

    @Nested
    class CompletedOnTime {

        @Test
        void onOrBeforeTheDueDate_isOnTime() {
            assertTrue(MaintenanceKpiService.isCompletedOnTime(due(completed(workOrder(0, Status.COMPLETE), 3), 3)));
            assertTrue(MaintenanceKpiService.isCompletedOnTime(due(completed(workOrder(0, Status.COMPLETE), 2), 3)));
        }

        @Test
        void afterTheDueDate_isLate() {
            assertFalse(MaintenanceKpiService.isCompletedOnTime(due(completed(workOrder(0, Status.COMPLETE), 4), 3)));
        }

        @Test
        void notCompletedOrWithoutDueDate_isNotOnTime() {
            assertFalse(MaintenanceKpiService.isCompletedOnTime(due(workOrder(0, Status.OPEN), 3)));
            assertFalse(MaintenanceKpiService.isCompletedOnTime(completed(workOrder(0, Status.COMPLETE), 1)));
        }
    }

    @Nested
    class Kpis {

        @Test
        void computesEveryKpi() {
            List<WorkOrder> pmDue = List.of(
                    due(completed(pm(workOrder(0, Status.COMPLETE)), 2), 3),
                    due(completed(pm(workOrder(0, Status.COMPLETE)), 5), 3),
                    due(pm(workOrder(0, Status.OPEN)), 3));
            List<WorkOrder> created = List.of(
                    pm(workOrder(0, Status.OPEN)),
                    workOrder(0, Status.OPEN),
                    workOrder(0, Status.OPEN),
                    workOrder(0, Status.OPEN));
            WorkOrder overdue = estimated(due(workOrder(0, Status.OPEN), 5), 2);
            WorkOrder onTrack = estimated(due(workOrder(0, Status.IN_PROGRESS), 20), 1.5);
            WorkOrder closed = completed(workOrder(0, Status.COMPLETE), 8);

            MaintenanceKpis kpis = MaintenanceKpiService.kpis(created, pmDue, List.of(overdue, onTrack, closed), at(10));

            assertEquals(at(10), kpis.getAsOf());
            assertEquals(3, kpis.getPmDue());
            assertEquals(1, kpis.getPmCompletedOnTime());
            assertEquals(100.0 / 3, kpis.getPmCompliance(), 1e-9);
            assertEquals(1, kpis.getPlanned());
            assertEquals(3, kpis.getReactive());
            assertEquals(25.0, kpis.getPlannedRatio(), 1e-9);
            assertEquals(2, kpis.getBacklog());
            assertEquals(3.5, kpis.getBacklogHours(), 1e-9);
            assertEquals(1, kpis.getOverdue());
        }

        @Test
        void withoutData_ratiosAreNull() {
            MaintenanceKpis kpis = MaintenanceKpiService.kpis(List.of(), List.of(), List.of(), at(0));

            assertNull(kpis.getPmCompliance());
            assertNull(kpis.getPlannedRatio());
            assertEquals(0, kpis.getBacklog());
            assertEquals(4, kpis.getByPriority().size());
            assertTrue(kpis.getByPriority().stream().allMatch(times -> times.getMtta() == null
                    && times.getMttr() == null && times.getCount() == 0));
        }
    }

    @Nested
    class ResponseTimes {

        @Test
        void averagesByPriority_highestFirst_reactiveOnly() {
            WorkOrder fast = reacted(completed(priority(workOrder(0, Status.COMPLETE), Priority.HIGH), 2.0 / 24),
                    0.5 / 24);
            WorkOrder slow = reacted(priority(workOrder(0, Status.IN_PROGRESS), Priority.HIGH), 1.5 / 24);
            WorkOrder preventive = reacted(completed(pm(priority(workOrder(0, Status.COMPLETE), Priority.HIGH)),
                    10), 9);
            WorkOrder withoutPriority = priority(workOrder(0, Status.OPEN), null);

            List<PriorityResponseTimes> times =
                    MaintenanceKpiService.responseTimesByPriority(List.of(fast, slow, preventive, withoutPriority));

            assertEquals(List.of(Priority.HIGH, Priority.MEDIUM, Priority.LOW, Priority.NONE),
                    times.stream().map(PriorityResponseTimes::getPriority).toList());
            PriorityResponseTimes high = times.get(0);
            assertEquals(2, high.getCount());
            assertEquals(1, high.getCompleted());
            assertEquals(1.0, high.getMtta(), 1e-9);
            assertEquals(2.0, high.getMttr(), 1e-9);
            assertEquals(1, times.get(3).getCount());
            assertNull(times.get(1).getMtta());
        }

        @Test
        void resolutionTime_startsAtTheRequest() {
            WorkOrder fromRequest = completed(priority(workOrder(1, Status.COMPLETE), Priority.LOW), 2);
            Request request = new Request();
            request.setCreatedAt(at(0));
            fromRequest.setParentRequest(request);

            PriorityResponseTimes low = MaintenanceKpiService.responseTimesByPriority(List.of(fromRequest)).get(2);

            assertEquals(48.0, low.getMttr(), 1e-9);
        }
    }

    @Nested
    class Trend {

        @Test
        void boundaries_oneSegmentPerDay_upToTheMaximum() {
            assertEquals(8, MaintenanceKpiService.boundaries(at(0), at(7), 15).size());
            assertEquals(16, MaintenanceKpiService.boundaries(at(0), at(60), 15).size());
            assertEquals(2, MaintenanceKpiService.boundaries(at(0), at(0.25), 15).size());
            List<Date> boundaries = MaintenanceKpiService.boundaries(at(0), at(60), 15);
            assertEquals(at(0), boundaries.get(0));
            assertEquals(at(60), boundaries.get(15));
        }

        @Test
        void backlogAndCreatedWorkOrders_bySegment() {
            WorkOrder openFromDay0 = workOrder(0, Status.OPEN);
            WorkOrder closedOnDay2 = completed(workOrder(0.5, Status.COMPLETE), 2);
            WorkOrder plannedOnDay3 = pm(workOrder(2.5, Status.OPEN));

            List<MaintenanceKpisByDate> trend = MaintenanceKpiService.trend(
                    List.of(openFromDay0, closedOnDay2, plannedOnDay3),
                    List.of(openFromDay0, closedOnDay2, plannedOnDay3),
                    at(0), at(3), 15);

            assertEquals(List.of(at(1), at(2), at(3)), trend.stream().map(MaintenanceKpisByDate::getDate).toList());
            assertEquals(List.of(2, 1, 2), trend.stream().map(MaintenanceKpisByDate::getBacklog).toList());
            // the first segment includes its start
            assertEquals(List.of(2, 0, 0), trend.stream().map(MaintenanceKpisByDate::getReactive).toList());
            assertEquals(List.of(0, 0, 1), trend.stream().map(MaintenanceKpisByDate::getPlanned).toList());
        }

        @Test
        void futurePeriod_isEmpty_withoutQueries() {
            Date tomorrow = new Date(System.currentTimeMillis() + DAY);

            assertTrue(maintenanceKpiService.getTrend(1L, tomorrow, new Date(tomorrow.getTime() + DAY),
                    AnalyticsFilter.NONE).isEmpty());
            verifyNoInteractions(workOrderRepository);
        }
    }

    @Nested
    class Availability {

        @Test
        void fromDowntimeAndLivingSeconds() {
            AssetAvailability availability = MaintenanceKpiService.availability(
                    new Object[]{7L, "Ventilator", 2L, new BigDecimal("25.4"), new BigDecimal("100")});

            assertEquals(7L, availability.getId());
            assertEquals("Ventilator", availability.getName());
            assertEquals(2, availability.getDowntimeEvents());
            assertEquals(25, availability.getDowntime());
            assertEquals(74.6, availability.getAvailability(), 1e-9);
        }

        @Test
        void neverBelowZero_andFullWithoutLivingTime() {
            assertEquals(0.0, MaintenanceKpiService.availability(new Object[]{1L, "A", 3L, 150.0, 100.0})
                    .getAvailability());
            assertEquals(100.0, MaintenanceKpiService.availability(new Object[]{1L, "A", 0L, 0.0, 0.0})
                    .getAvailability());
        }

        @Test
        void query_receivesTheDownStatusesAndAssetFilters() {
            AnalyticsFilter filter = new AnalyticsFilter(Set.of(3L), Set.of(5L), Set.of(7L), Set.of(), Set.of());
            when(assetDowntimeRepository.findAvailabilityByAsset(eq(1L), any(), any(), any(), any(), any(),
                    anyInt())).thenReturn(List.<Object[]>of(new Object[]{1L, "A", 0L, 0.0, 10.0}));

            List<AssetAvailability> result = maintenanceKpiService.getAvailabilityByAsset(1L, at(0),
                    new Date(), filter);

            assertEquals(1, result.size());
            verify(assetDowntimeRepository).findAvailabilityByAsset(eq(1L), eq(at(0)), any(),
                    eq(List.of(AssetStatus.DOWN.ordinal(), AssetStatus.EMERGENCY_SHUTDOWN.ordinal())),
                    eq(List.of(3L)), eq(List.of(5L)), eq(MaintenanceKpiService.AVAILABILITY_LIMIT));
        }
    }

    @Nested
    class Service {

        @Test
        void getKpis_appliesTheFilterToEveryQuery() {
            AnalyticsFilter highOnly = new AnalyticsFilter(Set.of(), Set.of(), Set.of(), Set.of(),
                    Set.of(Priority.HIGH));
            WorkOrder high = priority(workOrder(0, Status.OPEN), Priority.HIGH);
            WorkOrder low = priority(workOrder(0, Status.OPEN), Priority.LOW);
            when(workOrderRepository.findByCompany_IdAndCreatedAtBetween(eq(1L), any(), any()))
                    .thenReturn(List.of(high, low));
            when(workOrderRepository.findByCompany_IdAndParentPreventiveMaintenanceIsNotNullAndDueDateBetween(eq(1L),
                    any(), any())).thenReturn(List.of());
            when(workOrderRepository.findOpenDuring(eq(1L), any(), any())).thenReturn(List.of(high, low));

            MaintenanceKpis kpis = maintenanceKpiService.getKpis(1L, at(0), at(10), highOnly);

            assertEquals(1, kpis.getReactive());
            assertEquals(1, kpis.getBacklog());
        }

        @Test
        void invalidRange_isRejected() {
            CustomException missing = assertThrows(CustomException.class,
                    () -> maintenanceKpiService.getKpis(1L, null, at(1), AnalyticsFilter.NONE));
            CustomException reversed = assertThrows(CustomException.class,
                    () -> maintenanceKpiService.getKpis(1L, at(2), at(1), AnalyticsFilter.NONE));

            assertEquals(HttpStatus.BAD_REQUEST, missing.getHttpStatus());
            assertEquals(HttpStatus.BAD_REQUEST, reversed.getHttpStatus());
        }
    }

    private static Date at(double days) {
        return new Date(T0.getTime() + Math.round(days * DAY));
    }

    private static WorkOrder workOrder(double createdDay, Status status) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setCreatedAt(at(createdDay));
        workOrder.setStatus(status);
        workOrder.setPriority(Priority.NONE);
        return workOrder;
    }

    private static WorkOrder completed(WorkOrder workOrder, double day) {
        workOrder.setCompletedOn(at(day));
        return workOrder;
    }

    private static WorkOrder due(WorkOrder workOrder, double day) {
        workOrder.setDueDate(at(day));
        return workOrder;
    }

    private static WorkOrder reacted(WorkOrder workOrder, double day) {
        workOrder.setFirstTimeToReact(at(day));
        return workOrder;
    }

    private static WorkOrder pm(WorkOrder workOrder) {
        workOrder.setParentPreventiveMaintenance(new PreventiveMaintenance());
        return workOrder;
    }

    private static WorkOrder priority(WorkOrder workOrder, Priority priority) {
        workOrder.setPriority(priority);
        return workOrder;
    }

    private static WorkOrder estimated(WorkOrder workOrder, double hours) {
        workOrder.setEstimatedDuration(hours);
        return workOrder;
    }
}
