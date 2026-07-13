package com.grash.controller.analytics;

import com.grash.dto.DateRange;
import com.grash.dto.analytics.parts.*;
import com.grash.dto.analytics.workOrders.IncompleteWOByAsset;
import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.enums.Status;
import com.grash.security.CurrentUser;
import com.grash.service.*;
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
@RequestMapping("/analytics/parts")
@Api(tags = "PartAnalytics")
@RequiredArgsConstructor
public class PartAnalyticsController {

    private final UserService userService;
    private final AssetService assetService;
    private final PartCategoryService partCategoryService;
    private final WorkOrderCategoryService workOrderCategoryService;
    private final WorkOrderService workOrderService;
    private final PartConsumptionService partConsumptionService;

    @PostMapping("/consumptions/overview")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getPartStats",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<PartStats> getPartStats(@ApiIgnore @CurrentUser OwnUser user,
                                                  @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            Collection<PartConsumption> partConsumptions =
                    partConsumptionService.findByCompanyAndCreatedAtBetween(user.getCompany().getId(),
                            dateRange.getStart(), dateRange.getEnd());
            double totalConsumptionCost = partConsumptions.stream().mapToDouble(PartConsumption::getCost).sum();
            int consumedCount = partConsumptions.stream()
                    .filter(partConsumption -> partConsumption.getPart().getUnit() == null).mapToInt(partConsumption -> (int) partConsumption.getQuantity()).sum();

            return ResponseEntity.ok(PartStats.builder()
                    .consumedCount(consumedCount)
                    .totalConsumptionCost(totalConsumptionCost)
                    .build());
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/consumptions/pareto")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getPartPareto",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<List<PartConsumptionsByPart>> getPareto(@ApiIgnore @CurrentUser OwnUser user,
                                                                  @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            Collection<PartConsumption> partConsumptions = partConsumptionService.findByCompanyAndCreatedAtBetween
                            (user.getCompany().getId(), dateRange.getStart(), dateRange.getEnd())
                    .stream().filter(partConsumption -> partConsumption.getQuantity() != 0).collect(Collectors.toList());
            Set<Part> parts = new HashSet<>(partConsumptions.stream()
                    .map(PartConsumption::getPart)
                    .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparingLong(Part::getId)))));
            List<PartConsumptionsByPart> result = parts.stream().map(part -> {
                double cost =
                        partConsumptions.stream().filter(partConsumption -> partConsumption.getPart().getId().equals(part.getId())).mapToDouble(PartConsumption::getCost).sum();
                return PartConsumptionsByPart.builder()
                        .id(part.getId())
                        .name(part.getName())
                        .cost(cost).build();
            }).sorted(Comparator.comparing(PartConsumptionsByPart::getCost).reversed()).collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/consumptions/assets")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getConsumptionByAsset",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<Collection<PartConsumptionsByAsset>> getConsumptionByAsset(@ApiIgnore @CurrentUser OwnUser user,
                                                                                     @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            Collection<Asset> assets = assetService.findByCompanyAndBefore(user.getCompany().getId(),
                    dateRange.getEnd());
            Collection<PartConsumptionsByAsset> result = new ArrayList<>();
            for (Asset asset : assets) {
                Collection<WorkOrder> workOrders = workOrderService.findByAssetAndCreatedAtBetween(asset.getId(),
                        dateRange.getStart(), dateRange.getEnd());
                List<PartConsumption> partConsumptions =
                        partConsumptionService.findByWorkOrders(workOrders.stream().map(WorkOrder::getId).collect(Collectors.toList()));
                double cost = partConsumptions.stream().mapToDouble(PartConsumption::getCost).sum();
                result.add(PartConsumptionsByAsset.builder()
                        .cost(cost)
                        .name(asset.getName())
                        .id(asset.getId())
                        .build());
            }
            result =
                    result.stream().filter(partConsumptionsByAsset -> partConsumptionsByAsset.getCost() != 0).collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/consumptions/parts-category")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getConsumptionByPartCategory",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<Collection<PartConsumptionByCategory>> getConsumptionByPartCategory(@ApiIgnore @CurrentUser OwnUser user,
                                                                                              @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            Collection<PartCategory> partCategories =
                    partCategoryService.findByCompanySettings(user.getCompany().getCompanySettings().getId());
            Collection<PartConsumptionByCategory> result = new ArrayList<>();
            Collection<PartConsumption> partConsumptions =
                    partConsumptionService.findByCompanyAndCreatedAtBetween(user.getCompany().getId(),
                            dateRange.getStart(), dateRange.getEnd());
            for (PartCategory category : partCategories) {
                double cost =
                        partConsumptions.stream().filter(partConsumption -> partConsumption.getPart().getCategory() != null
                                && category.getId().equals(partConsumption.getPart().getCategory().getId())).mapToDouble(PartConsumption::getCost).sum();
                result.add(PartConsumptionByCategory.builder()
                        .cost(cost)
                        .name(category.getName())
                        .id(category.getId())
                        .build());
            }
            return ResponseEntity.ok(result);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/consumptions/work-order-category")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getConsumptionByWOCategory",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<Collection<PartConsumptionByWOCategory>> getConsumptionByWOCategory(@ApiIgnore @CurrentUser OwnUser user,
                                                                                              @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            Collection<WorkOrderCategory> workOrderCategories =
                    workOrderCategoryService.findByCompanySettings(user.getCompany().getCompanySettings().getId());
            Collection<PartConsumptionByWOCategory> result = new ArrayList<>();
            Collection<PartConsumption> partConsumptions =
                    partConsumptionService.findByCompanyAndCreatedAtBetween(user.getCompany().getId(),
                            dateRange.getStart(), dateRange.getEnd());
            for (WorkOrderCategory category : workOrderCategories) {
                double cost =
                        partConsumptions.stream().filter(partConsumption -> partConsumption.getWorkOrder().getCategory() != null
                                && category.getId().equals(partConsumption.getWorkOrder().getCategory().getId())).mapToDouble(PartConsumption::getCost).sum();
                result.add(PartConsumptionByWOCategory.builder()
                        .cost(cost)
                        .name(category.getName())
                        .id(category.getId())
                        .build());
            }
            return ResponseEntity.ok(result);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/consumptions/date")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getPartConsumptionsByMonth",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end)"
    )
    public ResponseEntity<List<PartConsumptionsByMonth>> getPartConsumptionsByMonth(@ApiIgnore @CurrentUser OwnUser user,
                                                                                    @RequestBody DateRange dateRange) {
        if (user.canSeeAnalytics()) {
            List<PartConsumptionsByMonth> result = new ArrayList<>();
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
                Collection<PartConsumption> partConsumptions =
                        partConsumptionService.findByCreatedAtBetweenAndCompany(Helper.localDateToDate(currentDate),
                                Helper.localDateToDate(nextDate), user.getCompany().getId());
                double cost = partConsumptions.stream().mapToDouble(PartConsumption::getCost).sum();
                result.add(PartConsumptionsByMonth.builder()
                        .cost(cost)
                        .date(Helper.localDateToDate(currentDate)).build());
                currentDate = nextDate;
            }
            return ResponseEntity.ok(result);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }
}
