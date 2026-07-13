package com.grash.controller.analytics;

import com.grash.dto.DateRange;
import com.grash.dto.analytics.workOrders.*;
import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.abstracts.Time;
import com.grash.model.abstracts.WorkOrderBase;
import com.grash.model.enums.Priority;
import com.grash.model.enums.Status;
import com.grash.model.envers.WorkOrderAud;
import com.grash.repository.WorkOrderAudRepository;
import com.grash.security.CurrentUser;
import com.grash.service.*;
import com.grash.utils.Helper;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/analytics/work-orders")
@Api(tags = "WorkOrderAnalytics")
@RequiredArgsConstructor
public class WOAnalyticsController {

    private final WorkOrderService workOrderService;
    private final WorkOrderAudRepository workOrderAudRepository;
    private final UserService userService;
    private final LaborService laborService;
    private final WorkOrderCategoryService workOrderCategoryService;
    private final AssetService assetService;

    @PostMapping("/complete/overview")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getCompleteStats",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<WOStats> getCompleteStats(@ApiIgnore @CurrentUser OwnUser user,
                                                    @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            Collection<WorkOrder> workOrders =
                    workOrderService.findByCompanyAndCreatedAtBetween(user.getCompany().getId(), dateRange.getStart()
                            , dateRange.getEnd());
            Collection<WorkOrder> completedWO =
                    workOrders.stream().filter(workOrder -> workOrder.getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());
            Collection<WorkOrder> withFirstTimeToReactWO =
                    workOrders.stream().filter(workOrder -> workOrder.getFirstTimeToReact() != null).collect(Collectors.toList());
            int total = workOrders.size();
            int complete = completedWO.size();
            int compliant = (int) completedWO.stream().filter(WorkOrder::isCompliant).count();
            long mtta = withFirstTimeToReactWO.isEmpty() ? 0 : withFirstTimeToReactWO.stream().mapToLong(workOrder ->
                    Helper.getDateDiff(workOrder.getCreatedAt(), workOrder.getFirstTimeToReact(), TimeUnit.HOURS)).sum() / withFirstTimeToReactWO.size();
            return ResponseEntity.ok(WOStats.builder()
                    .total(total)
                    .complete(complete)
                    .compliant(compliant)
                    .mtta(mtta)
                    .avgCycleTime(WorkOrder.getAverageAge(completedWO)).build());
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/mobile/overview")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<MobileWOStats> getMobileOverview(@ApiIgnore @CurrentUser OwnUser user,
                                                           @RequestParam("assignedToMe") boolean assignedToMe) {
        Collection<WorkOrder> result;
        Collection<WorkOrder> workOrders;
        Collection<WorkOrder> completeWorkOrders;
        if (assignedToMe) {
            result = workOrderService.findByAssignedToUser(user.getId());
        } else {
            result = workOrderService.findByCompany(user.getCompany().getId());
        }
        workOrders =
                result.stream().filter(workOrder -> !workOrder.isArchived() && !workOrder.getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());
        completeWorkOrders =
                result.stream().filter(workOrder -> workOrder.getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());

        int open = (int) workOrders.stream().filter(workOrder -> workOrder.getStatus().equals(Status.OPEN)).count();
        int onHold =
                (int) workOrders.stream().filter(workOrder -> workOrder.getStatus().equals(Status.ON_HOLD)).count();
        int inProgress =
                (int) workOrders.stream().filter(workOrder -> workOrder.getStatus().equals(Status.IN_PROGRESS)).count();
        int complete = completeWorkOrders.size();
        int todayWO = (int) workOrders.stream().filter(workOrder -> {
            LocalTime midnight = LocalTime.MIDNIGHT;
            LocalDate today = LocalDate.now(ZoneId.of("UTC"));
            LocalDateTime todayMidnight = LocalDateTime.of(today, midnight);
            LocalDateTime tomorrowMidnight = todayMidnight.plusDays(1);
            return workOrder.getDueDate() != null && workOrder.getDueDate().after(Helper.localDateTimeToDate(todayMidnight)) && workOrder.getDueDate().before(Helper.localDateTimeToDate(tomorrowMidnight));
        }).count();
        int high = (int) workOrders.stream().filter(workOrder -> workOrder.getPriority().equals(Priority.HIGH)).count();
        return ResponseEntity.ok(MobileWOStats.builder()
                .open(open)
                .onHold(onHold)
                .inProgress(inProgress)
                .complete(complete)
                .today(todayWO)
                .high(high).build());
    }

    @GetMapping("/mobile/complete-compliant")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<MobileWOStatsExtended> getMobileExtendedStats(@ApiIgnore @CurrentUser OwnUser user) {
        Date weekStart = Helper.localDateToDate(LocalDate.now().minusDays(7));
        Collection<WorkOrder> workOrders = workOrderService.findByCompany(user.getCompany().getId());
        Collection<WorkOrder> completeWO =
                workOrders.stream().filter(workOrder -> workOrder.getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());
        Collection<WorkOrder> compliantWO =
                completeWO.stream().filter(WorkOrder::isCompliant).collect(Collectors.toList());
        Collection<WorkOrder> completeWOWeek =
                completeWO.stream().filter(workOrder -> workOrder.getCompletedOn().before(new Date()) && workOrder.getCompletedOn().after(weekStart)).collect(Collectors.toList());
        Collection<WorkOrder> compliantWOWeek = compliantWO.stream().filter(workOrder -> {
            if (workOrder.getCompletedOn() == null) {
                return true;
            } else return workOrder.getCompletedOn().after(weekStart);
        }).collect(Collectors.toList());
        return ResponseEntity.ok(MobileWOStatsExtended.builder()
                .complete(completeWO.size())
                .completeWeek(completeWOWeek.size())
                .compliantRate(workOrders.isEmpty() ? 1 : ((double) compliantWO.size()) / workOrders.size())
                .compliantRateWeek(completeWOWeek.isEmpty() ? 1 :
                        ((double) compliantWOWeek.size()) / completeWOWeek.size())
                .build());
    }

    @PostMapping("/incomplete/overview")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getIncompleteStats",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<WOIncompleteStats> getIncompleteStats(@ApiIgnore @CurrentUser OwnUser user,
                                                                @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            Collection<WorkOrder> workOrders =
                    workOrderService.findByCompanyAndCreatedAtBetween(user.getCompany().getId(), dateRange.getStart()
                            , dateRange.getEnd());
            Collection<WorkOrder> incompleteWO =
                    workOrders.stream().filter(workOrder -> !workOrder.getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());
            int total = incompleteWO.size();
            List<Long> ages = incompleteWO.stream().map(workOrder -> Helper.getDateDiff(workOrder.getRealCreatedAt(),
                    new Date(), TimeUnit.DAYS)).collect(Collectors.toList());
            int averageAge = ages.size() == 0 ? 0 : ages.stream().mapToInt(Long::intValue).sum() / ages.size();
            return ResponseEntity.ok(WOIncompleteStats.builder()
                    .total(total)
                    .averageAge(averageAge)
                    .build());
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/incomplete/priority")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getIncompleteByPriority",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<WOStatsByPriority> getIncompleteByPriority(@ApiIgnore @CurrentUser OwnUser user,
                                                                     @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            Collection<WorkOrder> workOrders =
                    workOrderService.findByCompanyAndCreatedAtBetween(user.getCompany().getId(), dateRange.getStart()
                            , dateRange.getEnd());
            Collection<WorkOrder> incompleteWO =
                    workOrders.stream().filter(workOrder -> !workOrder.getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());

            Pair<Integer, Double> highValues = getCountsAndEstimatedDurationByPriority(Priority.HIGH, incompleteWO);
            Pair<Integer, Double> noneValues = getCountsAndEstimatedDurationByPriority(Priority.NONE, incompleteWO);
            Pair<Integer, Double> lowValues = getCountsAndEstimatedDurationByPriority(Priority.LOW, incompleteWO);
            Pair<Integer, Double> mediumValues = getCountsAndEstimatedDurationByPriority(Priority.MEDIUM, incompleteWO);

            int highCounts = highValues.getFirst();
            double highEstimatedDurations = highValues.getSecond();
            int mediumCounts = mediumValues.getFirst();
            double mediumEstimatedDurations = mediumValues.getSecond();
            int lowCounts = lowValues.getFirst();
            double lowEstimatedDurations = lowValues.getSecond();
            int noneCounts = noneValues.getFirst();
            double noneEstimatedDurations = noneValues.getSecond();

            return ResponseEntity.ok(WOStatsByPriority.builder()
                    .high(WOStatsByPriority.BasicStats.builder()
                            .count(highCounts)
                            .estimatedHours(highEstimatedDurations)
                            .build())
                    .none(WOStatsByPriority.BasicStats.builder()
                            .count(noneCounts)
                            .estimatedHours(noneEstimatedDurations)
                            .build())
                    .low(WOStatsByPriority.BasicStats.builder()
                            .count(lowCounts)
                            .estimatedHours(lowEstimatedDurations)
                            .build())
                    .medium(WOStatsByPriority.BasicStats.builder()
                            .count(mediumCounts)
                            .estimatedHours(mediumEstimatedDurations)
                            .build())
                    .build());

        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/incomplete/statuses")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getWOStatuses",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<WOStatuses> getWOStatuses(@ApiIgnore @CurrentUser OwnUser user,
                                                    @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            Collection<WorkOrder> workOrders =
                    workOrderService.findByCompanyAndCreatedAtBetween(user.getCompany().getId(), dateRange.getStart()
                            , dateRange.getEnd());
            Collection<WorkOrder> incompleteWO =
                    workOrders.stream().filter(workOrder -> !workOrder.getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());

            return ResponseEntity.ok(WOStatuses.builder()
                    .open(getWOCountsByStatus(Status.OPEN, incompleteWO))
                    .inProgress(getWOCountsByStatus(Status.IN_PROGRESS, incompleteWO))
                    .onHold(getWOCountsByStatus(Status.ON_HOLD, incompleteWO))
                    .complete(getWOCountsByStatus(Status.COMPLETE, incompleteWO))
                    .build());
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/incomplete/age/assets")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getIncompleteByAsset",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<Collection<IncompleteWOByAsset>> getIncompleteByAsset(@ApiIgnore @CurrentUser OwnUser user,
                                                                                @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            Collection<Asset> assets = assetService.findByCompanyAndBefore(user.getCompany().getId(),
                    dateRange.getEnd());
            Collection<IncompleteWOByAsset> result = new ArrayList<>();
            assets.forEach(asset -> {
                Collection<WorkOrder> incompleteWO = workOrderService.findByAssetAndCreatedAtBetween(asset.getId(),
                                dateRange.getStart(), dateRange.getEnd())
                        .stream().filter(workOrder -> !workOrder.getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());
                List<Long> ages = incompleteWO.stream().map(workOrder -> Helper.getDateDiff(workOrder.getCreatedAt(),
                        new Date(), TimeUnit.DAYS)).collect(Collectors.toList());
                int count = incompleteWO.size();
                result.add(IncompleteWOByAsset.builder()
                        .count(count)
                        .averageAge(count == 0 ? 0 : ages.stream().mapToLong(value -> value).sum() / count)
                        .name(asset.getName())
                        .id(asset.getId())
                        .build());
            });
            return ResponseEntity.ok(result);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/incomplete/age/users")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getIncompleteByUser",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<Collection<IncompleteWOByUser>> getIncompleteByUser(@ApiIgnore @CurrentUser OwnUser user,
                                                                              @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            Collection<OwnUser> users = userService.findWorkersByCompany(user.getCompany().getId());
            Collection<IncompleteWOByUser> result = new ArrayList<>();
            users.forEach(user1 -> {
                Collection<WorkOrder> incompleteWO =
                        workOrderService.findByAssignedToUserAndCreatedAtBetween(user1.getId(), dateRange.getStart(),
                                        dateRange.getEnd())
                                .stream().filter(workOrder -> !workOrder.getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());
                List<Long> ages = incompleteWO.stream().map(workOrder -> Helper.getDateDiff(workOrder.getCreatedAt(),
                        new Date(), TimeUnit.DAYS)).collect(Collectors.toList());
                int count = incompleteWO.size();
                result.add(IncompleteWOByUser.builder()
                        .count(count)
                        .averageAge(count == 0 ? 0 : ages.stream().mapToLong(value -> value).sum() / count)
                        .firstName(user1.getFirstName())
                        .lastName(user1.getLastName())
                        .id(user1.getId())
                        .build());
            });
            return ResponseEntity.ok(result);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/hours")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getWOHours",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<WOHours> getHours(@ApiIgnore @CurrentUser OwnUser user, @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            Collection<WorkOrder> workOrders =
                    workOrderService.findByCompanyAndCreatedAtBetween(user.getCompany().getId(), dateRange.getStart()
                            , dateRange.getEnd());
            double estimated =
                    workOrders.stream().map(WorkOrderBase::getEstimatedDuration).mapToDouble(value -> value).sum();
            Collection<Labor> labors = new ArrayList<>();
            workOrders.forEach(workOrder -> labors.addAll(laborService.findByWorkOrder(workOrder.getId())));
            int actual = labors.stream().map(Labor::getDuration).mapToInt(Math::toIntExact).sum() / 3600;
            return ResponseEntity.ok(WOHours.builder()
                    .estimated(estimated)
                    .actual(actual)
                    .build());
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/complete/counts/primaryUser")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getWOCountsByUser",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<Collection<WOCountByUser>> getCountsByUser(@ApiIgnore @CurrentUser OwnUser user,
                                                                     @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            Collection<OwnUser> users = userService.findWorkersByCompany(user.getCompany().getId());
            Collection<WOCountByUser> results = new ArrayList<>();
            users.forEach(user1 -> {
                int count = (int) workOrderService.findByAssignedToUserAndCreatedAtBetween(user1.getId(),
                                dateRange.getStart(), dateRange.getEnd()).stream()
                        .filter(workOrder -> workOrder.getStatus().equals(Status.COMPLETE)).count();
                results.add(WOCountByUser.builder()
                        .firstName(user1.getFirstName())
                        .lastName(user1.getLastName())
                        .id(user1.getId())
                        .count(count)
                        .build());
            });
            return ResponseEntity.ok(results);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/complete/counts/completedBy")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getWOCountsByCompletedBy",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<Collection<WOCountByUser>> getCountsByCompletedBy(@ApiIgnore @CurrentUser OwnUser user,
                                                                            @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            Collection<OwnUser> users = userService.findWorkersByCompany(user.getCompany().getId());
            Collection<WOCountByUser> results = new ArrayList<>();
            users.forEach(user1 -> {
                int count = (int) workOrderService.findByCompletedByAndCreatedAtBetween(user1.getId(),
                                dateRange.getStart(), dateRange.getEnd()).stream()
                        .filter(workOrder -> workOrder.getStatus().equals(Status.COMPLETE)).count();
                results.add(WOCountByUser.builder()
                        .firstName(user1.getFirstName())
                        .lastName(user1.getLastName())
                        .id(user1.getId())
                        .count(count)
                        .build());
            });
            return ResponseEntity.ok(results);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/complete/counts/priority")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getWOCountsByPriority",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<Map<Priority, Integer>> getCountsByPriority(@ApiIgnore @CurrentUser OwnUser user,
                                                                      @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            Priority[] priorities = Priority.values();
            Map<Priority, Integer> results = new HashMap<>();
            Arrays.asList(priorities).forEach(priority -> {
                int count = (int) workOrderService.findByPriorityAndCompanyAndCreatedAtBetween(priority,
                                user.getCompany().getId(), dateRange.getStart(), dateRange.getEnd()).stream()
                        .filter(workOrder -> workOrder.getStatus().equals(Status.COMPLETE)).count();
                results.put(priority, count);
            });
            return ResponseEntity.ok(results);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/complete/counts/category")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getWOCountsByCategory",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<Collection<WOCountByCategory>> getCountsByCategory(@ApiIgnore @CurrentUser OwnUser user,
                                                                             @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            Collection<WorkOrderCategory> categories =
                    workOrderCategoryService.findByCompanySettings(user.getCompany().getCompanySettings().getId());
            Collection<WOCountByCategory> results = new ArrayList<>();
            categories.forEach(category -> {
                int count = (int) workOrderService.findByCategoryAndCreatedAtBetween(category.getId(),
                                dateRange.getStart(), dateRange.getEnd()).stream()
                        .filter(workOrder -> workOrder.getStatus().equals(Status.COMPLETE)).count();
                results.add(WOCountByCategory.builder()
                        .name(category.getName())
                        .id(category.getId())
                        .count(count)
                        .build());
            });
            return ResponseEntity.ok(results);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/complete/counts/week")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getWOCompleteByWeek",
            key = "#user.id"
    )
    public ResponseEntity<List<WOCountByWeek>> getCompleteByWeek(@ApiIgnore @CurrentUser OwnUser user) {
        if (user.canSeeAnalytics()) {
            List<WOCountByWeek> result = new ArrayList<>();
            LocalDate previousMonday =
                    LocalDate.now(ZoneId.of("UTC"));
            // .with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
            for (int i = 0; i < 5; i++) {
                Collection<WorkOrder> completeWorkOrders =
                        workOrderService.findByCompletedOnBetweenAndCompany(Helper.localDateToDate(previousMonday.minusDays(7)), Helper.localDateToDate(previousMonday), user.getCompany().getId())
                                .stream().filter(workOrder -> workOrder.getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());
                int compliant = (int) completeWorkOrders.stream().filter(WorkOrder::isCompliant).count();
                int reactive = (int) completeWorkOrders.stream().filter(WorkOrder::isReactive).count();
                result.add(WOCountByWeek.builder()
                        .count(completeWorkOrders.size())
                        .compliant(compliant)
                        .reactive(reactive)
                        .date(Helper.localDateToDate(previousMonday)).build());
                previousMonday = previousMonday.minusDays(7);
            }
            Collections.reverse(result);
            return ResponseEntity.ok(result);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/complete/time/week")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getWOCompleteTimeByWeek",
            key = "#user.id"
    )
    public ResponseEntity<List<WOTimeByWeek>> getCompleteTimeByWeek(@ApiIgnore @CurrentUser OwnUser user) {
        if (user.canSeeAnalytics()) {
            List<WOTimeByWeek> result = new ArrayList<>();
            LocalDate previousMonday =
                    LocalDate.now(ZoneId.of("UTC"));
            // .with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
            for (int i = 0; i < 5; i++) {
                Collection<WorkOrder> completeWorkOrders =
                        workOrderService.findByCompletedOnBetweenAndCompany(Helper.localDateToDate(previousMonday.minusDays(7)), Helper.localDateToDate(previousMonday), user.getCompany().getId())
                                .stream().filter(workOrder -> workOrder.getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());
                Collection<WorkOrder> reactiveWorkOrders =
                        completeWorkOrders.stream().filter(WorkOrder::isReactive).collect(Collectors.toList());

                long total = getTime(completeWorkOrders);
                long reactive = getTime(reactiveWorkOrders);
                result.add(WOTimeByWeek.builder()
                        .total(total)
                        .reactive(reactive)
                        .date(Helper.localDateToDate(previousMonday)).build());
                previousMonday = previousMonday.minusDays(7);
            }
            Collections.reverse(result);
            return ResponseEntity.ok(result);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/complete/costs-time")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getWOCompleteCostsAndTime",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<WOCostsAndTime> getCompleteCostsAndTime(@ApiIgnore @CurrentUser OwnUser user,
                                                                  @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            Collection<WorkOrder> completeWorkOrders = workOrderService.findByCompanyAndCreatedAtBetween(
                    user.getCompany().getId(), dateRange.getStart(), dateRange.getEnd()).stream().filter(workOrder -> workOrder.getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());
            double additionalCost = workOrderService.getAdditionalCost(completeWorkOrders);
            long laborCost = workOrderService.getLaborCostAndTime(completeWorkOrders).getFirst();
            long laborTime = workOrderService.getLaborCostAndTime(completeWorkOrders).getSecond();
            double partCost = workOrderService.getPartCost(completeWorkOrders);
            double total = laborCost + partCost + additionalCost;

            return ResponseEntity.ok(WOCostsAndTime.builder()
                    .total(total)
                    .average(completeWorkOrders.size() == 0 ? 0 : total / completeWorkOrders.size())
                    .additionalCost(additionalCost)
                    .laborCost(laborCost)
                    .partCost(partCost)
                    .laborTime(laborTime)
                    .build());
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/complete/costs/date")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getWOCompleteCostsByDate",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<List<WOCostsByDate>> getCompleteCostsByDate(@ApiIgnore @CurrentUser OwnUser user,
                                                                      @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            LocalDate endDateLocale = Helper.dateToLocalDate(dateRange.getEnd());
            List<WOCostsByDate> result = new ArrayList<>();
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
                Collection<WorkOrder> completeWorkOrders =
                        workOrderService.findByCompletedOnBetweenAndCompany(Helper.localDateToDate(currentDate),
                                        Helper.localDateToDate(nextDate), user.getCompany().getId())
                                .stream().filter(workOrder -> workOrder.getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());
                result.add(WOCostsByDate.builder()
                        .additionalCost(workOrderService.getAdditionalCost(completeWorkOrders))
                        .laborCost(workOrderService.getLaborCostAndTime(completeWorkOrders).getFirst())
                        .partCost(workOrderService.getPartCost(completeWorkOrders))
                        .date(Helper.localDateToDate(currentDate)).build());
                currentDate = nextDate;
            }
            return ResponseEntity.ok(result);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/statuses")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getWOStatusesByDate",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<List<WOStatusesByDate>> getReceivedAndResolvedForDateRange(@ApiIgnore @CurrentUser OwnUser user,
                                                                                     @RequestBody DateRange dateRange) {
        LocalDate endDateLocale = Helper.dateToLocalDate(dateRange.getEnd());
        if (user.canSeeAnalytics()) {
            List<WOStatusesByDate> result = new ArrayList<>();
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
                Collection<WorkOrder> workOrders =
                        workOrderService.findByCompanyAndCreatedAtBetween(user.getCompany().getId(),
                                dateRange.getStart(), Helper.localDateToDate(nextDate));
                LocalDate finalNextDate = nextDate;
                Collection<Status> statuses = workOrders.stream().map(workOrder -> {
                    List<WorkOrderAud> workOrderAuds = workOrderAudRepository.findLastByIdAndDate(workOrder.getId(),
                            Helper.localDateToDate(finalNextDate).getTime(), PageRequest.of(0, 1));
                    if (workOrderAuds.isEmpty()) return workOrder.getStatus();
                    else return workOrderAuds.get(0).getStatus();
                }).filter(Objects::nonNull).collect(Collectors.toList());

                result.add(WOStatusesByDate.builder()
                        .open((int) statuses.stream().filter(status -> status.equals(Status.OPEN)).count())
                        .onHold((int) statuses.stream().filter(status -> status.equals(Status.ON_HOLD)).count())
                        .inProgress((int) statuses.stream().filter(status -> status.equals(Status.IN_PROGRESS)).count())
                        .complete((int) statuses.stream().filter(status -> status.equals(Status.COMPLETE)).count())
                        .date(Helper.localDateToDate(currentDate))
                        .build());
                currentDate = nextDate; // Move to the next segment
            }
            return ResponseEntity.ok(result);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    private Pair<Integer, Double> getCountsAndEstimatedDurationByPriority(Priority priority,
                                                                          Collection<WorkOrder> workOrders) {
        Collection<WorkOrder> priorityWO =
                workOrders.stream().filter(workOrder -> workOrder.getPriority().equals(priority)).collect(Collectors.toList());
        int priorityCounts = priorityWO.size();
        double priorityEstimatedDurations =
                priorityWO.stream().map(WorkOrderBase::getEstimatedDuration).mapToDouble(value -> value).sum();
        return Pair.of(priorityCounts, priorityEstimatedDurations);
    }

    private int getWOCountsByStatus(Status status, Collection<WorkOrder> workOrders) {
        Collection<WorkOrder> statusWO =
                workOrders.stream().filter(workOrder -> workOrder.getStatus().equals(status)).collect(Collectors.toList());
        return statusWO.size();
    }

    private long getTime(Collection<WorkOrder> workOrders) {
        Collection<Labor> labors = new ArrayList<>();
        workOrders.forEach(workOrder -> {
            labors.addAll(laborService.findByWorkOrder(workOrder.getId()));
        });
        return labors.stream().mapToLong(Time::getDuration).sum();
    }
}
