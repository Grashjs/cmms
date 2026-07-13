package com.grash.controller.analytics;

import com.grash.dto.DateRange;
import com.grash.dto.analytics.assets.*;
import com.grash.exception.CustomException;
import com.grash.model.Asset;
import com.grash.model.AssetDowntime;
import com.grash.model.OwnUser;
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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/analytics/assets")
@Api(tags = "AssetAnalytics")
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
    public ResponseEntity<Collection<TimeCostByAsset>> getTimeCostByAsset(@ApiIgnore @CurrentUser OwnUser user,
                                                                          @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            Collection<Asset> assets = assetService.findByCompanyAndBefore(user.getCompany().getId(),
                    dateRange.getEnd());
            Collection<TimeCostByAsset> result = new ArrayList<>();
            assets.forEach(asset -> {
                Collection<WorkOrder> completeWO = workOrderService.findByAssetAndCreatedAtBetween(asset.getId(),
                                dateRange.getStart(), dateRange.getEnd())
                        .stream().filter(workOrder -> workOrder.getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());
                long time = workOrderService.getLaborCostAndTime(completeWO).getSecond();
                double cost = workOrderService.getAllCost(completeWO,
                        user.getCompany().getCompanySettings().getGeneralPreferences().isLaborCostInTotalCost());
                result.add(TimeCostByAsset.builder()
                        .time(time)
                        .cost(cost)
                        .name(asset.getName())
                        .id(asset.getId())
                        .build());
            });
            return ResponseEntity.ok(result);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/overview")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getOverviewStats",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<AssetStats> getOverviewStats(@ApiIgnore @CurrentUser OwnUser user,
                                                       @RequestBody DateRange dateRange) {
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
    public ResponseEntity<Collection<DowntimesByAsset>> getDowntimesByAsset(@ApiIgnore @CurrentUser OwnUser user,
                                                                            @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            Collection<Asset> assets = assetService.findByCompanyAndBefore(user.getCompany().getId(),
                    dateRange.getEnd());
            return ResponseEntity.ok(assets.stream().map(asset -> {
                Collection<AssetDowntime> downtimes =
                        assetDowntimeService.findByAssetAndStartsOnBetween(asset.getId(), dateRange.getStart(),
                                dateRange.getEnd());
                long downtimesDuration =
                        downtimes.stream().mapToLong(assetDowntime -> assetDowntime.getDateRangeDuration(dateRange)).sum();
                long percent = downtimesDuration * 100 / getLivingTime(asset, dateRange);
                return DowntimesByAsset.builder()
                        .count(downtimes.size())
                        .percent(percent)
                        .id(asset.getId())
                        .name(asset.getName())
                        .build();
            }).collect(Collectors.toList()));
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/mtbf")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getMTBFByAsset",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<Collection<MTBFByAsset>> getMTBFByAsset(@CurrentUser OwnUser user,
                                                                  @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            Collection<Asset> assets = assetService.findByCompanyAndBefore(user.getCompany().getId(),
                    dateRange.getEnd());
            return ResponseEntity.ok(assets.stream().map(asset -> MTBFByAsset.builder()
                    .mtbf(assetService.getMTBF(asset.getId(), dateRange.getStart(), dateRange.getEnd()))
                    .id(asset.getId())
                    .name(asset.getName())
                    .build()).collect(Collectors.toList()));
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/meantimes")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getMeantimes",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<Meantimes> getMeantimes(@ApiIgnore @CurrentUser OwnUser user,
                                                  @RequestBody DateRange dateRange) {
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
    public ResponseEntity<Collection<RepairTimeByAsset>> getRepairTimeByAsset(@ApiIgnore @CurrentUser OwnUser user,
                                                                              @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            Collection<Asset> assets = assetService.findByCompanyAndBefore(user.getCompany().getId(),
                    dateRange.getEnd());
            return ResponseEntity.ok(assets.stream().map(asset -> {
                Collection<WorkOrder> completeWO = workOrderService.findByAssetAndCreatedAtBetween(asset.getId(),
                        dateRange.getStart(), dateRange.getEnd()).stream().filter(workOrder -> workOrder.getStatus().equals(Status.COMPLETE)).collect(Collectors.toList());
                return RepairTimeByAsset.builder()
                        .id(asset.getId())
                        .name(asset.getName())
                        .duration(WorkOrder.getAverageAge(completeWO))
                        .build();
            }).collect(Collectors.toList()));
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/downtimes/meantime/date")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getDowntimesMeantimeByMonth",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<List<DowntimesMeantimeByDate>> getDowntimesMeantimeByMonth(@ApiIgnore @CurrentUser OwnUser user,
                                                                                     @RequestBody DateRange dateRange) {
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
    public ResponseEntity<AssetsCosts> getAssetsCosts(@ApiIgnore @CurrentUser OwnUser user,
                                                      @RequestBody DateRange dateRange) {
        boolean includeLaborCost =
                user.getCompany().getCompanySettings().getGeneralPreferences().isLaborCostInTotalCost();
        if (user.canSeeAnalytics()) {
            Collection<Asset> assets = assetService.findByCompanyAndBefore(user.getCompany().getId(),
                    dateRange.getEnd());
            Collection<Asset> assetsWithAcquisitionCost =
                    assets.stream().filter(asset -> asset.getAcquisitionCost() != null).collect(Collectors.toList());
            double totalAcquisitionCost =
                    assetsWithAcquisitionCost.stream().mapToDouble(Asset::getAcquisitionCost).sum();
            double totalWOCosts = getCompleteWOCosts(assets, includeLaborCost, dateRange);
            double rav = assetsWithAcquisitionCost.isEmpty() ? 0 : getCompleteWOCosts(assetsWithAcquisitionCost,
                    includeLaborCost, dateRange) * 100 / totalAcquisitionCost;
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
    public ResponseEntity<Collection<DowntimesAndCostsByAsset>> getDowntimesAndCosts(@ApiIgnore @CurrentUser OwnUser user,
                                                                                     @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            Collection<Asset> assets = assetService.findByCompanyAndBefore(user.getCompany().getId(),
                    dateRange.getEnd());
            return ResponseEntity.ok(assets.stream().map(asset -> {
                Collection<AssetDowntime> downtimes = assetDowntimeService.findByAsset(asset.getId()).stream()
                        .filter(assetDowntime -> assetDowntime.getDuration() != 0).collect(Collectors.toList());
                long downtimesDuration =
                        downtimes.stream().mapToLong(assetDowntime -> assetDowntime.getDateRangeDuration(dateRange)).sum();
                double totalWOCosts = getCompleteWOCosts(Collections.singleton(asset),
                        user.getCompany().getCompanySettings().getGeneralPreferences().isLaborCostInTotalCost(),
                        dateRange);
                return DowntimesAndCostsByAsset.builder()
                        .id(asset.getId())
                        .name(asset.getName())
                        .duration(downtimesDuration)
                        .workOrdersCosts(totalWOCosts)
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
    public ResponseEntity<List<DowntimesByDate>> getDowntimesByMonth(@ApiIgnore @CurrentUser OwnUser user,
                                                                     @RequestBody DateRange dateRange) {
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
    public ResponseEntity<AssetOverview> getDateRangeOverview(@PathVariable Long id, @RequestBody DateRange dateRange
            , @ApiIgnore @CurrentUser OwnUser user) {
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

    private double getCompleteWOCosts(Collection<Asset> assets, boolean includeLaborCost, DateRange dateRange) {
        return assets.stream().map(asset -> workOrderService.findByAssetAndCreatedAtBetween(asset.getId(),
                dateRange.getStart(), dateRange.getEnd()).stream().filter(workOrder -> workOrder.getStatus().equals(Status.COMPLETE)).collect(Collectors.toList())).mapToDouble(workOrder -> workOrderService.getAllCost(workOrder, includeLaborCost)).sum();
    }

    private long getLivingTime(Asset asset, DateRange dateRange) {
        return Helper.getDateDiff(asset.getRealCreatedAt()
                .before(dateRange.getStart()) ? dateRange.getStart()
                : asset.getRealCreatedAt(), dateRange.getEnd(), TimeUnit.SECONDS);
    }
}
