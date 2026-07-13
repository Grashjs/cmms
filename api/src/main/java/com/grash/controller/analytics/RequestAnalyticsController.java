package com.grash.controller.analytics;

import com.grash.dto.DateRange;
import com.grash.dto.analytics.requests.*;
import com.grash.exception.CustomException;
import com.grash.model.OwnUser;
import com.grash.model.Request;
import com.grash.model.WorkOrder;
import com.grash.model.WorkOrderCategory;
import com.grash.model.enums.Priority;
import com.grash.model.enums.Status;
import com.grash.security.CurrentUser;
import com.grash.service.RequestService;
import com.grash.service.UserService;
import com.grash.service.WorkOrderCategoryService;
import com.grash.utils.Helper;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/analytics/requests")
@Api(tags = "RequestAnalytics")
@RequiredArgsConstructor
public class RequestAnalyticsController {

    private final UserService userService;
    private final WorkOrderCategoryService workOrderCategoryService;
    private final RequestService requestService;

    @PostMapping("/overview")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getRequestStats",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<RequestStats> getRequestStats(@ApiIgnore @CurrentUser OwnUser user,
                                                        @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            Collection<Request> requests = requestService.findByCreatedAtBetweenAndCompany(dateRange.getStart(),
                    dateRange.getEnd(), user.getCompany().getId());
            Collection<Request> approvedRequests =
                    requests.stream().filter(request -> request.getWorkOrder() != null).collect(Collectors.toList());
            Collection<Request> cancelledRequests =
                    requests.stream().filter(Request::isCancelled).collect(Collectors.toList());
            Collection<Request> pendingRequests =
                    requests.stream().filter(request -> request.getWorkOrder() == null && !request.isCancelled()).collect(Collectors.toList());
            Collection<Request> completeRequests =
                    approvedRequests.stream().filter(request -> request.getWorkOrder().getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());
            long cycleTime =
                    WorkOrder.getAverageAge(completeRequests.stream().map(Request::getWorkOrder).collect(Collectors.toList()));
            return ResponseEntity.ok(RequestStats.builder()
                    .approved(approvedRequests.size())
                    .pending(pendingRequests.size())
                    .cancelled(cancelledRequests.size())
                    .cycleTime(cycleTime)
                    .build());
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/priority")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getRequestByPriority",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<RequestStatsByPriority> getByPriority(@ApiIgnore @CurrentUser OwnUser user,
                                                                @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            Collection<Request> requests = requestService.findByCreatedAtBetweenAndCompany(dateRange.getStart(),
                    dateRange.getEnd(), user.getCompany().getId());

            int highCounts = getCountsByPriority(Priority.HIGH, requests);
            int noneCounts = getCountsByPriority(Priority.NONE, requests);
            int lowCounts = getCountsByPriority(Priority.LOW, requests);
            int mediumCounts = getCountsByPriority(Priority.MEDIUM, requests);

            return ResponseEntity.ok(RequestStatsByPriority.builder()
                    .high(RequestStatsByPriority.BasicStats.builder()
                            .count(highCounts)
                            .build())
                    .none(RequestStatsByPriority.BasicStats.builder()
                            .count(noneCounts)
                            .build())
                    .low(RequestStatsByPriority.BasicStats.builder()
                            .count(lowCounts)
                            .build())
                    .medium(RequestStatsByPriority.BasicStats.builder()
                            .count(mediumCounts)
                            .build())
                    .build());

        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/cycle-time/date")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getCycleTimeByMonth",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<List<RequestsByMonth>> getCycleTimeByMonth(@ApiIgnore @CurrentUser OwnUser user,
                                                                     @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            List<RequestsByMonth> result = new ArrayList<>();
            LocalDate endDateLocale = Helper.dateToLocalDate(dateRange.getEnd());
            LocalDate currentDate = Helper.dateToLocalDate(dateRange.getStart());
            LocalDate endDateExclusive = Helper.dateToLocalDate(dateRange.getEnd()).plusDays(1); // Include end date
            // in the range
            long totalDaysInRange = ChronoUnit.DAYS.between(Helper.dateToLocalDate(dateRange.getStart()),
                    endDateExclusive);
            int points = Math.toIntExact(Math.min(15, totalDaysInRange));

            for (int i = 0; i < points; i++) {
                LocalDate nextDate = currentDate.plusDays(totalDaysInRange / points); // Distribute evenly over the
                // range
                nextDate = nextDate.isAfter(endDateLocale) ? endDateLocale : nextDate; // Adjust for the end date
                Collection<Request> requests =
                        requestService.findByCreatedAtBetweenAndCompany(Helper.localDateToDate(currentDate),
                                Helper.localDateToDate(nextDate), user.getCompany().getId());
                Collection<Request> completeRequests =
                        requests.stream().filter(request -> request.getWorkOrder() != null && request.getWorkOrder().getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());
                long cycleTime =
                        WorkOrder.getAverageAge(completeRequests.stream().map(Request::getWorkOrder).collect(Collectors.toList()));
                result.add(RequestsByMonth.builder()
                        .cycleTime(cycleTime)
                        .date(Helper.localDateToDate(currentDate)).build());
                currentDate = nextDate;
            }
            return ResponseEntity.ok(result);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/counts/category")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getRequestCountsByCategory",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<Collection<CountByCategory>> getCountsByCategory(@ApiIgnore @CurrentUser OwnUser user,
                                                                           @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            Collection<WorkOrderCategory> categories =
                    workOrderCategoryService.findByCompanySettings(user.getCompany().getCompanySettings().getId());
            Collection<CountByCategory> results = new ArrayList<>();
            categories.forEach(category -> {
                int count = requestService.findByCategoryAndCreatedAtBetween(category.getId(), dateRange.getStart(),
                        dateRange.getEnd()).size();
                results.add(CountByCategory.builder()
                        .name(category.getName())
                        .id(category.getId())
                        .count(count)
                        .build());
            });
            return ResponseEntity.ok(results);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/received-and-resolved")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getReceivedAndResolvedRequests",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<List<RequestsResolvedByDate>> getReceivedAndResolvedForDateRange(@ApiIgnore @CurrentUser OwnUser user,
                                                                                           @RequestBody DateRange dateRange) {
        LocalDate endDateLocale = Helper.dateToLocalDate(dateRange.getEnd());
        if (user.canSeeAnalytics()) {
            List<RequestsResolvedByDate> result = new ArrayList<>();
            LocalDate currentDate = Helper.dateToLocalDate(dateRange.getStart());
            LocalDate endDateExclusive = Helper.dateToLocalDate(dateRange.getEnd()).plusDays(1); // Include end date
            // in the range
            long totalDaysInRange = ChronoUnit.DAYS.between(Helper.dateToLocalDate(dateRange.getStart()),
                    endDateExclusive);
            int points = Math.toIntExact(Math.min(15, totalDaysInRange));

            for (int i = 0; i < points; i++) {
                LocalDate nextDate = currentDate.plusDays(totalDaysInRange / points); // Distribute evenly over the
                // range
                nextDate = nextDate.isAfter(endDateLocale) ? endDateLocale : nextDate; // Adjust for the end date
                Collection<Request> notCancelledRequests =
                        requestService.findByCreatedAtBetweenAndCompany(Helper.localDateToDate(currentDate),
                                        Helper.localDateToDate(nextDate), user.getCompany().getId())
                                .stream().filter(request -> !request.isCancelled()).collect(Collectors.toList());
                Collection<Request> completeRequests =
                        notCancelledRequests.stream().filter(request -> request.getWorkOrder() != null && request.getWorkOrder().getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());
                int resolved = completeRequests.size();
                int received = notCancelledRequests.size();
                result.add(RequestsResolvedByDate.builder()
                        .resolved(resolved)
                        .received(received)
                        .date(Helper.localDateToDate(currentDate))
                        .build());
                currentDate = nextDate; // Move to the next segment
            }
            return ResponseEntity.ok(result);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    private int getCountsByPriority(Priority priority, Collection<Request> requests) {
        return (int) requests.stream().filter(request -> request.getPriority().equals(priority)).count();
    }
}
