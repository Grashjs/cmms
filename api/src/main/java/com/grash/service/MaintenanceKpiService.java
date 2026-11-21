package com.grash.service;

import com.grash.dto.analytics.AnalyticsFilter;
import com.grash.dto.analytics.assets.AssetAvailability;
import com.grash.dto.analytics.workOrders.MaintenanceKpis;
import com.grash.dto.analytics.workOrders.MaintenanceKpisByDate;
import com.grash.dto.analytics.workOrders.PriorityResponseTimes;
import com.grash.exception.CustomException;
import com.grash.model.WorkOrder;
import com.grash.model.enums.AssetStatus;
import com.grash.model.enums.Priority;
import com.grash.model.enums.Status;
import com.grash.repository.AssetDowntimeRepository;
import com.grash.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

/**
 * Maintenance KPIs of the Analytics module (issue #318): preventive maintenance compliance, planned vs reactive
 * work, backlog, overdue work orders, response and resolution times by priority, and asset availability.
 * <p>
 * The backlog at an instant is rebuilt from {@code createdAt}, {@code completedOn} and the current status, without
 * the audit history: a work order that was completed and then reopened counts as open since its creation.
 */
@Service
@RequiredArgsConstructor
public class MaintenanceKpiService {

    static final int MAX_TREND_POINTS = 15;
    static final int AVAILABILITY_LIMIT = 50;
    static final List<Integer> DOWN_STATUSES = Arrays.stream(AssetStatus.values())
            .filter(AssetStatus::isReallyDown)
            .map(Enum::ordinal)
            .toList();

    private static final double HOUR_MS = TimeUnit.HOURS.toMillis(1);
    private static final double DAY_MS = TimeUnit.DAYS.toMillis(1);
    // highest first, as the reports show them
    private static final List<Priority> PRIORITY_ORDER = List.of(Priority.HIGH, Priority.MEDIUM, Priority.LOW,
            Priority.NONE);

    private final WorkOrderRepository workOrderRepository;
    private final AssetDowntimeRepository assetDowntimeRepository;

    public MaintenanceKpis getKpis(Long companyId, Date start, Date end, AnalyticsFilter filter) {
        requireRange(start, end);
        Date asOf = earliest(end, new Date());
        List<WorkOrder> created =
                filter.apply(workOrderRepository.findByCompany_IdAndCreatedAtBetween(companyId, start, end));
        List<WorkOrder> pmDue = asOf.before(start) ? List.of() : filter.apply(workOrderRepository
                .findByCompany_IdAndParentPreventiveMaintenanceIsNotNullAndDueDateBetween(companyId, start, asOf));
        List<WorkOrder> open = filter.apply(workOrderRepository.findOpenDuring(companyId, asOf, asOf));
        return kpis(created, pmDue, open, asOf);
    }

    public List<MaintenanceKpisByDate> getTrend(Long companyId, Date start, Date end, AnalyticsFilter filter) {
        requireRange(start, end);
        Date effectiveEnd = earliest(end, new Date());
        if (!effectiveEnd.after(start)) return List.of();
        List<WorkOrder> created =
                filter.apply(workOrderRepository.findByCompany_IdAndCreatedAtBetween(companyId, start, effectiveEnd));
        List<WorkOrder> openDuring = filter.apply(workOrderRepository.findOpenDuring(companyId, start, effectiveEnd));
        return trend(created, openDuring, start, effectiveEnd, MAX_TREND_POINTS);
    }

    /**
     * Availability of the assets matching the location and asset category filters, worst first. The team, work
     * order category and priority filters do not apply to assets.
     */
    public List<AssetAvailability> getAvailabilityByAsset(Long companyId, Date start, Date end,
                                                          AnalyticsFilter filter) {
        requireRange(start, end);
        Date effectiveEnd = earliest(end, new Date());
        if (!effectiveEnd.after(start)) return List.of();
        return assetDowntimeRepository.findAvailabilityByAsset(companyId, start, effectiveEnd, DOWN_STATUSES,
                        filter.locationIdsParam(), filter.assetCategoryIdsParam(), AVAILABILITY_LIMIT)
                .stream()
                .map(MaintenanceKpiService::availability)
                .toList();
    }

    static MaintenanceKpis kpis(Collection<WorkOrder> created, Collection<WorkOrder> pmDue,
                                Collection<WorkOrder> open, Date asOf) {
        int pmCompletedOnTime = (int) pmDue.stream().filter(MaintenanceKpiService::isCompletedOnTime).count();
        int planned = (int) created.stream().filter(workOrder -> !workOrder.isReactive()).count();
        List<WorkOrder> backlog = open.stream().filter(workOrder -> isOpenAt(workOrder, asOf)).toList();
        return MaintenanceKpis.builder()
                .asOf(asOf)
                .pmDue(pmDue.size())
                .pmCompletedOnTime(pmCompletedOnTime)
                .pmCompliance(percent(pmCompletedOnTime, pmDue.size()))
                .planned(planned)
                .reactive(created.size() - planned)
                .plannedRatio(percent(planned, created.size()))
                .backlog(backlog.size())
                .backlogHours(estimatedHours(backlog))
                .overdue(overdue(backlog, asOf))
                .byPriority(responseTimesByPriority(created))
                .build();
    }

    static List<MaintenanceKpisByDate> trend(Collection<WorkOrder> created, Collection<WorkOrder> openDuring,
                                             Date start, Date end, int maxPoints) {
        List<Date> boundaries = boundaries(start, end, maxPoints);
        List<MaintenanceKpisByDate> result = new ArrayList<>();
        for (int i = 1; i < boundaries.size(); i++) {
            Date from = boundaries.get(i - 1);
            Date to = boundaries.get(i);
            boolean includeFrom = i == 1;
            List<WorkOrder> backlog = openDuring.stream().filter(workOrder -> isOpenAt(workOrder, to)).toList();
            List<WorkOrder> createdInSegment = created.stream()
                    .filter(workOrder -> isWithin(workOrder.getCreatedAt(), from, to, includeFrom))
                    .toList();
            int planned = (int) createdInSegment.stream().filter(workOrder -> !workOrder.isReactive()).count();
            result.add(MaintenanceKpisByDate.builder()
                    .date(to)
                    .backlog(backlog.size())
                    .backlogHours(estimatedHours(backlog))
                    .overdue(overdue(backlog, to))
                    .planned(planned)
                    .reactive(createdInSegment.size() - planned)
                    .build());
        }
        return result;
    }

    /**
     * Splits [start, end] into equal segments: one per day, up to {@code maxPoints}.
     *
     * @return the boundaries of the segments, from start to end
     */
    static List<Date> boundaries(Date start, Date end, int maxPoints) {
        long span = end.getTime() - start.getTime();
        long days = Math.max(1, (long) Math.ceil(span / DAY_MS));
        int segments = (int) Math.min(maxPoints, days);
        List<Date> boundaries = new ArrayList<>();
        for (int i = 0; i <= segments; i++) {
            boundaries.add(new Date(start.getTime() + span * i / segments));
        }
        return boundaries;
    }

    /**
     * Whether the work order was open at the instant: created before it, not archived, and not completed before
     * it.
     */
    static boolean isOpenAt(WorkOrder workOrder, Date instant) {
        if (workOrder.isArchived() || workOrder.getCreatedAt() == null || workOrder.getCreatedAt().after(instant)) {
            return false;
        }
        boolean completedBefore = workOrder.getStatus() == Status.COMPLETE
                && (workOrder.getCompletedOn() == null || !workOrder.getCompletedOn().after(instant));
        return !completedBefore;
    }

    static boolean isCompletedOnTime(WorkOrder workOrder) {
        return workOrder.getStatus() == Status.COMPLETE && workOrder.getCompletedOn() != null
                && workOrder.getDueDate() != null && !workOrder.getCompletedOn().after(workOrder.getDueDate());
    }

    /**
     * Reactive work orders only: preventive work has its own compliance KPI and would hide the response to
     * failures.
     */
    static List<PriorityResponseTimes> responseTimesByPriority(Collection<WorkOrder> created) {
        List<WorkOrder> reactive = created.stream().filter(WorkOrder::isReactive).toList();
        return PRIORITY_ORDER.stream().map(priority -> {
            List<WorkOrder> ofPriority = reactive.stream()
                    .filter(workOrder -> priorityOf(workOrder) == priority)
                    .toList();
            List<WorkOrder> completed = ofPriority.stream()
                    .filter(workOrder -> workOrder.getStatus() == Status.COMPLETE && workOrder.getCompletedOn() != null)
                    .toList();
            return PriorityResponseTimes.builder()
                    .priority(priority)
                    .count(ofPriority.size())
                    .completed(completed.size())
                    .mtta(averageHours(ofPriority.stream()
                            .filter(workOrder -> workOrder.getFirstTimeToReact() != null
                                    && workOrder.getCreatedAt() != null)
                            .mapToLong(workOrder -> workOrder.getFirstTimeToReact().getTime()
                                    - workOrder.getCreatedAt().getTime())))
                    .mttr(averageHours(completed.stream()
                            .filter(workOrder -> workOrder.getRealCreatedAt() != null)
                            .mapToLong(workOrder -> workOrder.getCompletedOn().getTime()
                                    - workOrder.getRealCreatedAt().getTime())))
                    .build();
        }).toList();
    }

    /**
     * @param row id, name, downtime events, downtime seconds, living seconds
     */
    static AssetAvailability availability(Object[] row) {
        double downtime = ((Number) row[3]).doubleValue();
        double living = ((Number) row[4]).doubleValue();
        // overlapping downtimes can add up to more than the living time
        double availability = living <= 0 ? 100 : Math.max(0, living - downtime) * 100 / living;
        return AssetAvailability.builder()
                .id(((Number) row[0]).longValue())
                .name((String) row[1])
                .downtimeEvents(((Number) row[2]).intValue())
                .downtime(Math.round(downtime))
                .availability(availability)
                .build();
    }

    private static Priority priorityOf(WorkOrder workOrder) {
        return workOrder.getPriority() == null ? Priority.NONE : workOrder.getPriority();
    }

    private static boolean isWithin(Date date, Date from, Date to, boolean includeFrom) {
        if (date == null || date.after(to)) return false;
        return includeFrom ? !date.before(from) : date.after(from);
    }

    private static double estimatedHours(Collection<WorkOrder> workOrders) {
        return workOrders.stream().mapToDouble(WorkOrder::getEstimatedDuration).sum();
    }

    private static int overdue(Collection<WorkOrder> openWorkOrders, Date instant) {
        return (int) openWorkOrders.stream()
                .filter(workOrder -> workOrder.getDueDate() != null && workOrder.getDueDate().before(instant))
                .count();
    }

    private static Double percent(int part, int total) {
        return total == 0 ? null : part * 100.0 / total;
    }

    private static Double averageHours(LongStream millis) {
        OptionalDouble average = millis.average();
        return average.isPresent() ? average.getAsDouble() / HOUR_MS : null;
    }

    private static Date earliest(Date first, Date second) {
        return first.before(second) ? first : second;
    }

    private static void requireRange(Date start, Date end) {
        if (start == null || end == null || start.after(end)) {
            throw new CustomException("A valid start and end are required", HttpStatus.BAD_REQUEST);
        }
    }
}
