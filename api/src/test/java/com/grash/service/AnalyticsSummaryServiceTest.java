package com.grash.service;

import com.grash.advancedsearch.FilterField;
import com.grash.dto.analytics.AnalyticsFilter;
import com.grash.dto.analytics.requests.RequestStats;
import com.grash.dto.analytics.summary.AnalyticsSummary;
import com.grash.dto.analytics.workOrders.MaintenanceKpis;
import com.grash.dto.analytics.workOrders.WOCostsAndTime;
import com.grash.dto.analytics.workOrders.WOStats;
import com.grash.model.*;
import com.grash.model.enums.Priority;
import com.grash.model.enums.Status;
import com.grash.repository.AssetCategoryRepository;
import com.grash.repository.TeamRepository;
import com.grash.repository.WorkOrderCategoryRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsSummaryServiceTest {

    private static final long HOUR = 3_600_000L;

    @Mock
    private WorkOrderService workOrderService;
    @Mock
    private RequestService requestService;
    @Mock
    private MaintenanceKpiService maintenanceKpiService;
    @Mock
    private LocationService locationService;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private AssetCategoryRepository assetCategoryRepository;
    @Mock
    private WorkOrderCategoryRepository workOrderCategoryRepository;
    @Mock
    private ResourceBundleMessageSource emailMessageSource;

    @InjectMocks
    private AnalyticsSummaryService analyticsSummaryService;

    @Nested
    class Stats {

        @Test
        void workOrderStats() {
            WorkOrder completedOnTime = workOrder(Status.COMPLETE);
            completedOnTime.setCompletedOn(new Date(48 * HOUR));
            completedOnTime.setFirstTimeToReact(new Date(HOUR / 2));
            WorkOrder open = workOrder(Status.OPEN);
            open.setFirstTimeToReact(new Date(3 * HOUR / 2));

            WOStats stats = AnalyticsSummaryService.workOrderStats(List.of(completedOnTime, open));

            assertEquals(2, stats.getTotal());
            assertEquals(1, stats.getComplete());
            assertEquals(1, stats.getCompliant());
            assertEquals(1.0, stats.getMtta(), 1e-9);
            assertEquals(2.0, stats.getAvgCycleTime(), 1e-9);
        }

        @Test
        void requestStats() {
            Request approved = new Request();
            WorkOrder workOrder = workOrder(Status.COMPLETE);
            workOrder.setCompletedOn(new Date(24 * HOUR));
            approved.setWorkOrder(workOrder);
            Request cancelled = new Request();
            cancelled.setCancelled(true);
            Request pending = new Request();

            RequestStats stats = AnalyticsSummaryService.requestStats(List.of(approved, cancelled, pending));

            assertEquals(1, stats.getApproved());
            assertEquals(1, stats.getCancelled());
            assertEquals(1, stats.getPending());
            assertEquals(1.0, stats.getCycleTime(), 1e-9);
        }
    }

    @Test
    void previousPeriod_hasTheSameLengthAndEndsBeforeTheStart() {
        Date start = new Date(10 * HOUR);
        Date end = new Date(20 * HOUR);

        Date[] previous = AnalyticsSummaryService.previousPeriod(start, end);

        assertEquals(new Date(10 * HOUR - 1), previous[1]);
        assertEquals(end.getTime() - start.getTime(), previous[1].getTime() - previous[0].getTime());
    }

    @Test
    void build_collectsBothPeriodsWithTheSameFilter() {
        Date start = new Date(10 * HOUR);
        Date end = new Date(20 * HOUR);
        Date[] previous = AnalyticsSummaryService.previousPeriod(start, end);
        AnalyticsFilter filter = new AnalyticsFilter(Set.of(), Set.of(), Set.of(7L), Set.of(), Set.of());
        when(maintenanceKpiService.getKpis(eq(1L), any(), any(), eq(filter))).thenReturn(new MaintenanceKpis());
        when(workOrderService.getCostsAndTime(any())).thenReturn(new WOCostsAndTime());
        when(maintenanceKpiService.getAvailabilityByAsset(1L, start, end, filter)).thenReturn(List.of());

        AnalyticsSummary summary = analyticsSummaryService.build(1L, start, end, filter);

        assertEquals(start, summary.current().start());
        assertEquals(previous[0], summary.previous().start());
        verify(maintenanceKpiService).getKpis(1L, start, end, filter);
        verify(maintenanceKpiService).getKpis(1L, previous[0], previous[1], filter);
        verify(workOrderService).findByCompanyAndCreatedAtBetween(1L, previous[0], previous[1]);
        verify(requestService).findByCreatedAtBetweenAndCompany(previous[0], previous[1], 1L);
    }

    @Nested
    class DescribeFilters {

        @Test
        void namesOnlyTheCompanysItems() {
            Company company = company();
            when(locationService.findByIdsAndCompany(List.of(3L, 99L), 1L)).thenReturn(List.of(location("UTI")));
            when(teamRepository.findByCompany_Id(1L)).thenReturn(List.of(team(7L, "Biomedica"), team(8L, "Otro")));
            when(emailMessageSource.getMessage(anyString(), isNull(), anyString(), any(Locale.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            List<String> lines = analyticsSummaryService.describeFilters(company, List.of(
                    in("location", 3, 99),
                    in("team", 7, 42),
                    in("priority", "HIGH")), Locale.ENGLISH);

            assertEquals(List.of("summary_filter_location: UTI", "summary_filter_team: Biomedica",
                    "summary_filter_priority: HIGH"), lines);
        }

        @Test
        void noFilters_noLines() {
            assertTrue(analyticsSummaryService.describeFilters(company(), null, Locale.ENGLISH).isEmpty());
            verifyNoInteractions(locationService, teamRepository);
        }
    }

    private static WorkOrder workOrder(Status status) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setStatus(status);
        workOrder.setPriority(Priority.NONE);
        workOrder.setCreatedAt(new Date(0));
        return workOrder;
    }

    private static Company company() {
        CompanySettings settings = new CompanySettings();
        settings.setId(5L);
        Company company = new Company();
        company.setId(1L);
        company.setCompanySettings(settings);
        return company;
    }

    private static Location location(String name) {
        Location location = new Location();
        location.setName(name);
        return location;
    }

    private static Team team(Long id, String name) {
        Team team = new Team();
        team.setId(id);
        team.setName(name);
        return team;
    }

    private static FilterField in(String field, Object... values) {
        return FilterField.builder().field(field).operation("in").value("")
                .values(new ArrayList<>(List.of(values))).build();
    }
}
