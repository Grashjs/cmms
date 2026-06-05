package com.grash.controller.analytics;

import com.grash.dto.DateRange;
import com.grash.dto.analytics.assets.*;
import com.grash.exception.CustomException;
import com.grash.model.Asset;
import com.grash.model.AssetDowntime;
import com.grash.model.User;
import com.grash.model.WorkOrder;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.Status;
import com.grash.security.CurrentUser;
import com.grash.service.AssetDowntimeService;
import com.grash.service.AssetService;
import com.grash.service.UserService;
import com.grash.service.WorkOrderService;
import com.grash.utils.AuditComparator;
import com.grash.utils.Helper;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Parameter;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/analytics/assets")
@Tag(name = "Asset Analytics", description = "Analytics operations on assets")
@RequiredArgsConstructor
public class AssetAnalyticsController {

    private final WorkOrderService workOrderService;
    private final UserService userService;
    private final AssetService assetService;
    private final AssetDowntimeService assetDowntimeService;

    @PostMapping("/time-cost")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getTimeCostByAsset",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<Collection<TimeCostByAsset>> getTimeCostByAsset(@Parameter(hidden = true) @CurrentUser User user,
                                                                          @Parameter(description = "Date range for " +
                                                                                  "filtering analytics") @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            boolean includeLaborCost =
                    user.getCompany().getCompanySettings().getGeneralPreferences().isLaborCostInTotalCost();
            List<Object[]> rows = workOrderService.findTopNAssetsTimeCost(
                    user.getCompany().getId(), dateRange.getStart(), dateRange.getEnd(), 10);
            Collection<TimeCostByAsset> result = rows.stream()
                    .map(row -> {
                        long time = ((Number) row[2]).longValue();
                        double laborCost = ((Number) row[3]).doubleValue();
                        double partCost = ((Number) row[4]).doubleValue();
                        double additionalCost = ((Number) row[5]).doubleValue();
                        double cost = partCost + additionalCost + (includeLaborCost ? laborCost : 0);
                        return TimeCostByAsset.builder()
                                .time(time)
                                .cost(cost)
                                .name((String) row[1])
                                .id((Long) row[0])
                                .build();
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/overview")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getOverviewStats",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<AssetStats> getOverviewStats(@Parameter(hidden = true) @CurrentUser User user,
                                                       @Parameter(description = "Date range for filtering analytics") @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            Collection<AssetDowntime> downtimes =
                    assetDowntimeService.findByCompanyAndStartsOnBetween(user.getCompany().getId(),
                            dateRange.getStart(), dateRange.getEnd());
            long downtimesDuration =
                    downtimes.stream().mapToLong(assetDowntime -> assetDowntime.getDateRangeDuration(dateRange)).sum();
            Collection<Asset> assets = assetService.findByCompanyAndBefore(user.getCompany().getId(),
                    dateRange.getEnd());
            long livingTime = assets.stream().mapToLong(asset -> getLivingTime(asset, dateRange)).sum();
            long availability = livingTime == 0 ? 0 : (livingTime - downtimesDuration) * 100 / livingTime;
            return ResponseEntity.ok(AssetStats.builder()
                    .downtime(downtimesDuration)
                    .availability(availability)
                    .downtimeEvents(downtimes.size())
                    .build());
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/downtimes")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getDowntimesByAsset",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<Collection<DowntimesByAsset>> getDowntimesByAsset(@Parameter(hidden = true) @CurrentUser User user,
                                                                            @Parameter(description = "Date range for " +
                                                                                    "filtering analytics") @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            List<Object[]> rows = assetDowntimeService.findTopNAssetsByDowntime(
                    user.getCompany().getId(), dateRange.getStart(), dateRange.getEnd(), 10);
            Collection<DowntimesByAsset> result = rows.stream().map(row -> {
                long cnt = ((Number) row[2]).longValue();
                long totalDuration = ((Number) row[3]).longValue();
                long livingTime = ((Number) row[4]).longValue();
                long percent = livingTime == 0 ? 0 : totalDuration * 100 / livingTime;
                return DowntimesByAsset.builder()
                        .count((int) cnt)
                        .percent(percent)
                        .id((Long) row[0])
                        .name((String) row[1])
                        .build();
            }).collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/mtbf")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getMTBFByAsset",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<Collection<MTBFByAsset>> getMTBFByAsset(@CurrentUser User user,
                                                                  @Parameter(description = "Date range for filtering " +
                                                                          "analytics") @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            List<Object[]> rows = assetDowntimeService.findTopNAssetsForMTBF(
                    user.getCompany().getId(), dateRange.getStart(), dateRange.getEnd(), 10);
            Collection<MTBFByAsset> result = rows.stream().map(row -> {
                Long assetId = (Long) row[0];
                return MTBFByAsset.builder()
                        .mtbf(assetService.getMTBF(assetId, dateRange.getStart(), dateRange.getEnd()))
                        .id(assetId)
                        .name((String) row[1])
                        .build();
            }).collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/meantimes")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getMeantimes",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<Meantimes> getMeantimes(@Parameter(hidden = true) @CurrentUser User user,
                                                  @Parameter(description = "Date range for filtering analytics") @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            Collection<AssetDowntime> downtimes =
                    assetDowntimeService.findByCompanyAndStartsOnBetween(user.getCompany().getId(),
                            dateRange.getStart(), dateRange.getEnd());
            long betweenMaintenances = 0L;
            Collection<WorkOrder> workOrders =
                    workOrderService.findByCompanyAndCreatedAtBetween(user.getCompany().getId(), dateRange.getStart()
                            , dateRange.getEnd());
            if (workOrders.size() > 2) {
                AuditComparator auditComparator = new AuditComparator();
                WorkOrder firstWorkOrder = Collections.min(workOrders, auditComparator);
                WorkOrder lastWorkOrder = Collections.max(workOrders, auditComparator);
                betweenMaintenances = (Helper.getDateDiff(firstWorkOrder.getCreatedAt(), lastWorkOrder.getCreatedAt()
                        , TimeUnit.HOURS)) / (workOrders.size() - 1);
            }
            return ResponseEntity.ok(Meantimes.builder()
                    .betweenDowntimes(assetDowntimeService.getDowntimesMeantime(downtimes))
                    .betweenMaintenances(betweenMaintenances)
                    .build());
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/repair-times")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getRepairTimeByAsset",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<Collection<RepairTimeByAsset>> getRepairTimeByAsset(@Parameter(hidden = true) @CurrentUser User user,
                                                                              @Parameter(description = "Date range " +
                                                                                      "for filtering analytics") @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            List<Object[]> rows = workOrderService.findTopNAssetsRepairTime(
                    user.getCompany().getId(), dateRange.getStart(), dateRange.getEnd(), 10);
            Collection<RepairTimeByAsset> result = rows.stream().map(row ->
                    RepairTimeByAsset.builder()
                            .id((Long) row[0])
                            .name((String) row[1])
                            .duration(((Number) row[2]).longValue())
                            .build()
            ).collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/downtimes/meantime/date")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getDowntimesMeantimeByMonth",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<List<DowntimesMeantimeByDate>> getDowntimesMeantimeByMonth(@Parameter(hidden = true) @CurrentUser User user,
                                                                                     @Parameter(description = "Date " +
                                                                                             "range for filtering " +
                                                                                             "analytics") @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            LocalDate endDateLocale = Helper.dateToLocalDate(dateRange.getEnd());
            List<DowntimesMeantimeByDate> result = new ArrayList<>();
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
                Collection<AssetDowntime> downtimes =
                        assetDowntimeService.findByStartsOnBetweenAndCompany(Helper.localDateToDate(currentDate),
                                Helper.localDateToDate(nextDate), user.getCompany().getId());
                result.add(DowntimesMeantimeByDate.builder()
                        .meantime(assetDowntimeService.getDowntimesMeantime(downtimes))
                        .date(Helper.localDateToDate(currentDate)).build());
                currentDate = nextDate;
            }
            return ResponseEntity.ok(result);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/costs/overview")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getAssetsCosts",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<AssetsCosts> getAssetsCosts(@Parameter(hidden = true) @CurrentUser User user,
                                                      @Parameter(description = "Date range for filtering analytics") @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            boolean includeLaborCost =
                    user.getCompany().getCompanySettings().getGeneralPreferences().isLaborCostInTotalCost();
            Long companyId = user.getCompany().getId();
            double totalAcquisitionCost = assetService.getTotalAcquisitionCost(companyId, dateRange.getEnd());
            Object[] totals = workOrderService.findTotalWOCosts(companyId, dateRange.getStart(), dateRange.getEnd()).get(0);
            double totalLabor = ((Number) totals[0]).doubleValue();
            double totalPart = ((Number) totals[1]).doubleValue();
            double totalAdd = ((Number) totals[2]).doubleValue();
            double laborWithAcq = ((Number) totals[3]).doubleValue();
            double partWithAcq = ((Number) totals[4]).doubleValue();
            double addWithAcq = ((Number) totals[5]).doubleValue();
            double totalWOCosts = totalPart + totalAdd + (includeLaborCost ? totalLabor : 0);
            double ravWOCosts = partWithAcq + addWithAcq + (includeLaborCost ? laborWithAcq : 0);
            double rav = totalAcquisitionCost == 0 ? 0 : ravWOCosts * 100 / totalAcquisitionCost;
            return ResponseEntity.ok(AssetsCosts.builder()
                    .totalWOCosts(totalWOCosts)
                    .totalAcquisitionCost(totalAcquisitionCost)
                    .rav(rav).build());
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/downtimes/costs")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getDowntimesAndCosts",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<Collection<DowntimesAndCostsByAsset>> getDowntimesAndCosts(@Parameter(hidden = true) @CurrentUser User user,
                                                                                     @Parameter(description = "Date " +
                                                                                             "range for filtering " +
                                                                                             "analytics") @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            boolean includeLaborCost =
                    user.getCompany().getCompanySettings().getGeneralPreferences().isLaborCostInTotalCost();
            List<Object[]> rows = assetDowntimeService.findTopNAssetsDowntimeAndCosts(
                    user.getCompany().getId(), dateRange.getStart(), dateRange.getEnd(), 10);
            return ResponseEntity.ok(rows.stream().map(row -> {
                long duration = ((Number) row[2]).longValue();
                double laborCost = ((Number) row[3]).doubleValue();
                double partCost = ((Number) row[4]).doubleValue();
                double additionalCost = ((Number) row[5]).doubleValue();
                double cost = partCost + additionalCost + (includeLaborCost ? laborCost : 0);
                return DowntimesAndCostsByAsset.builder()
                        .id((Long) row[0])
                        .name((String) row[1])
                        .duration(duration)
                        .workOrdersCosts(cost)
                        .build();
            }).collect(Collectors.toList()));
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/downtimes/costs/date")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getDowntimesByMonth",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<List<DowntimesByDate>> getDowntimesByMonth(@Parameter(hidden = true) @CurrentUser User user,
                                                                     @Parameter(description = "Date range for " +
                                                                             "filtering analytics") @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            List<DowntimesByDate> result = new ArrayList<>();
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
                Collection<WorkOrder> completeWorkOrders =
                        workOrderService.findByCompletedOnBetweenAndCompany(Helper.localDateToDate(currentDate),
                                        Helper.localDateToDate(nextDate), user.getCompany().getId())
                                .stream().filter(workOrder -> workOrder.getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());
                Collection<AssetDowntime> downtimes =
                        assetDowntimeService.findByStartsOnBetweenAndCompany(Helper.localDateToDate(currentDate),
                                Helper.localDateToDate(nextDate), user.getCompany().getId());
                result.add(DowntimesByDate.builder()
                        .workOrdersCosts(workOrderService.getAllCost(completeWorkOrders,
                                user.getCompany().getCompanySettings().getGeneralPreferences().isLaborCostInTotalCost()))
                        .duration(downtimes.stream().mapToLong(AssetDowntime::getDuration).sum())
                        .date(Helper.localDateToDate(currentDate)).build());
                currentDate = nextDate;
            }
            return ResponseEntity.ok(result);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/{id}/overview")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getDateRangeOverview",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)+'_'+#id"
    )
    public ResponseEntity<AssetOverview> getDateRangeOverview(@PathVariable Long id, @Parameter(description = "Date " +
                                                                      "range for filtering analytics") @RequestBody DateRange dateRange
            , @Parameter(hidden = true) @CurrentUser User user) {
        Asset savedAsset = assetService.findById(id).get();
        Date start = dateRange.getStart();
        Date end = dateRange.getEnd();
        if (user.getRole().getViewPermissions().contains(PermissionEntity.ASSETS) &&
                (user.getRole().getViewOtherPermissions().contains(PermissionEntity.ASSETS) || savedAsset.getCreatedBy().equals(user.getId()))) {
            AssetOverview result = AssetOverview.builder()
                    .mttr(assetService.getMTTR(id, start, end))
                    .mtbf(assetService.getMTBF(id, start, end))
                    .downtime(assetService.getDowntime(id, start, end))
                    .uptime(assetService.getUptime(id, start, end))
                    .totalCost(assetService.getTotalCost(id, start, end,
                            user.getCompany().getCompanySettings().getGeneralPreferences().isLaborCostInTotalCost()))
                    .build();
            return ResponseEntity.ok(result);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    private long getLivingTime(Asset asset, DateRange dateRange) {
        return Helper.getDateDiff(asset.getRealCreatedAt()
                .before(dateRange.getStart()) ? dateRange.getStart()
                : asset.getRealCreatedAt(), dateRange.getEnd(), TimeUnit.SECONDS);
    }
}

