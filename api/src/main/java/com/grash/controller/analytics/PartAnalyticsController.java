package com.grash.controller.analytics;

import com.grash.dto.DateRange;
import com.grash.dto.analytics.parts.*;
import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.security.CurrentUser;
import com.grash.service.*;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/analytics/parts")
@Tag(name = "Part Analytics", description = "Analytics operations on parts")
@RequiredArgsConstructor
public class PartAnalyticsController {

    private final UserService userService;
    private final AssetService assetService;
    private final PartCategoryService partCategoryService;
    private final WorkOrderCategoryService workOrderCategoryService;
    private final WorkOrderService workOrderService;
    private final PartTransactionService partTransactionService;
    private final CompanyService companyService;

    @PostMapping("/consumptions/overview")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getPartStats",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end) + " +
                    "(#companyId != null ? '_' + #companyId : '')"
    )
    public ResponseEntity<PartStats> getPartStats(@Parameter(hidden = true) @CurrentUser User user,
                                                  @Parameter(description = "Date range for filtering analytics") @RequestBody DateRange dateRange,
                                                  @RequestParam(required = false) @Parameter(description = "Filter by specific company") Long companyId) {
        if (user.canSeeAnalytics()) {
            Long resolvedCompanyId = Helper.resolveCompanyId(user, companyId);
            Collection<PartTransaction> partTransactions =
                    partTransactionService.findConsumptionsByCompanyAndCreatedAtBetween(resolvedCompanyId,
                            dateRange.getStart(), dateRange.getEnd());
            double totalConsumptionCost = partTransactions.stream().mapToDouble(PartTransaction::getCost).sum();
            int consumedCount = partTransactions.stream()
                    .filter(partTransaction -> partTransaction.getPart().getUnit() == null).mapToInt(partTransaction -> (int) partTransaction.getQuantity()).sum();

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
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end) + " +
                    "(#companyId != null ? '_' + #companyId : '')"
    )
    public ResponseEntity<List<PartConsumptionsByPart>> getPareto(@Parameter(hidden = true) @CurrentUser User user,
                                                                  @Parameter(description = "Date range for filtering " +
                                                                          "analytics") @RequestBody DateRange dateRange,
                                                                  @RequestParam(required = false) @Parameter(description = "Filter by specific company") Long companyId) {
        if (user.canSeeAnalytics()) {
            Long resolvedCompanyId = Helper.resolveCompanyId(user, companyId);
            Collection<PartTransaction> partTransactions =
                    partTransactionService.findConsumptionsByCompanyAndCreatedAtBetween
                                    (resolvedCompanyId, dateRange.getStart(), dateRange.getEnd())
                            .stream().filter(partTransaction -> partTransaction.getQuantity() != 0).collect(Collectors.toList());
            Set<Part> parts = new HashSet<>(partTransactions.stream()
                    .map(PartTransaction::getPart)
                    .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparingLong(Part::getId)))));
            List<PartConsumptionsByPart> result = parts.stream().map(part -> {
                double cost =
                        partTransactions.stream().filter(partTransaction -> partTransaction.getPart().getId().equals(part.getId())).mapToDouble(PartTransaction::getCost).sum();
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
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end) + " +
                    "(#companyId != null ? '_' + #companyId : '')"
    )
    public ResponseEntity<Collection<PartConsumptionsByAsset>> getConsumptionByAsset(@Parameter(hidden = true) @CurrentUser User user,
                                                                                     @Parameter(description = "Date " +
                                                                                             "range for filtering " +
                                                                                             "analytics") @RequestBody DateRange dateRange,
                                                                                     @RequestParam(required = false) @Parameter(description = "Filter by specific company") Long companyId) {
        if (user.canSeeAnalytics()) {
            Long resolvedCompanyId = Helper.resolveCompanyId(user, companyId);
            List<Object[]> rows = partTransactionService.findTopNAssetsByConsumption(
                    resolvedCompanyId, dateRange.getStart(), dateRange.getEnd(), 10);
            return ResponseEntity.ok(rows.stream().map(row ->
                    PartConsumptionsByAsset.builder()
                            .id((Long) row[0])
                            .name((String) row[1])
                            .cost(((Number) row[2]).doubleValue())
                            .build()
            ).collect(Collectors.toList()));
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/consumptions/parts-category")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getConsumptionByPartCategory",
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end) + " +
                    "(#companyId != null ? '_' + #companyId : '')"
    )
    public ResponseEntity<Collection<PartConsumptionByCategory>> getConsumptionByPartCategory(@Parameter(hidden =
                                                                                                       true) @CurrentUser User user,
                                                                                               @Parameter(description
                                                                                                       = "Date range " +
                                                                                                       "for filtering " +
                                                                                                       "analytics") @RequestBody DateRange dateRange,
                                                                                               @RequestParam(required = false) @Parameter(description = "Filter by specific company") Long companyId) {
        if (user.canSeeAnalytics()) {
            Long resolvedCompanyId = Helper.resolveCompanyId(user, companyId);
            Collection<PartCategory> partCategories =
                    partCategoryService.findByCompanySettings(
                            companyService.findById(resolvedCompanyId)
                                    .orElseThrow(() -> new CustomException("Company not found", HttpStatus.NOT_FOUND))
                                    .getCompanySettings().getId());
            Collection<PartConsumptionByCategory> result = new ArrayList<>();
            Collection<PartTransaction> partTransactions =
                    partTransactionService.findConsumptionsByCompanyAndCreatedAtBetween(resolvedCompanyId,
                            dateRange.getStart(), dateRange.getEnd());
            for (PartCategory category : partCategories) {
                double cost =
                        partTransactions.stream().filter(partTransaction -> partTransaction.getPart().getCategory() != null
                                && category.getId().equals(partTransaction.getPart().getCategory().getId())).mapToDouble(PartTransaction::getCost).sum();
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
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end) + " +
                    "(#companyId != null ? '_' + #companyId : '')"
    )
    public ResponseEntity<Collection<PartConsumptionByWOCategory>> getConsumptionByWOCategory(@Parameter(hidden =
                                                                                                       true) @CurrentUser User user,
                                                                                               @Parameter(description
                                                                                                       = "Date range " +
                                                                                                       "for filtering" +
                                                                                                       " " +
                                                                                                       "analytics") @RequestBody DateRange dateRange,
                                                                                               @RequestParam(required = false) @Parameter(description = "Filter by specific company") Long companyId) {
        if (user.canSeeAnalytics()) {
            Long resolvedCompanyId = Helper.resolveCompanyId(user, companyId);
            Collection<WorkOrderCategory> workOrderCategories =
                    workOrderCategoryService.findByCompanySettings(
                            companyService.findById(resolvedCompanyId)
                                    .orElseThrow(() -> new CustomException("Company not found", HttpStatus.NOT_FOUND))
                                    .getCompanySettings().getId());
            Collection<PartConsumptionByWOCategory> result = new ArrayList<>();
            Collection<PartTransaction> partTransactions =
                    partTransactionService.findConsumptionsByCompanyAndCreatedAtBetween(resolvedCompanyId,
                            dateRange.getStart(), dateRange.getEnd());
            for (WorkOrderCategory category : workOrderCategories) {
                double cost =
                        partTransactions.stream().filter(partTransaction -> partTransaction.getWorkOrder().getCategory() != null
                                && category.getId().equals(partTransaction.getWorkOrder().getCategory().getId())).mapToDouble(PartTransaction::getCost).sum();
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
            key = "T(com.grash.utils.CacheKeyUtils).dateRangeKey(#user.id, #dateRange.start, #dateRange.end) + " +
                    "(#companyId != null ? '_' + #companyId : '')"
    )
    public ResponseEntity<List<PartConsumptionsByMonth>> getPartConsumptionsByMonth(@Parameter(hidden = true) @CurrentUser User user,
                                                                                    @Parameter(description = "Date " +
                                                                                            "range for filtering " +
                                                                                            "analytics") @RequestBody DateRange dateRange,
                                                                                    @RequestParam(required = false) @Parameter(description = "Filter by specific company") Long companyId) {
        if (user.canSeeAnalytics()) {
            Long resolvedCompanyId = Helper.resolveCompanyId(user, companyId);
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
                Collection<PartTransaction> partTransactions =
                        partTransactionService.findConsumptionsByCompanyAndCreatedAtBetween(resolvedCompanyId
                                , Helper.localDateToDate(currentDate),
                                Helper.localDateToDate(nextDate));
                double cost = partTransactions.stream().mapToDouble(PartTransaction::getCost).sum();
                result.add(PartConsumptionsByMonth.builder()
                        .cost(cost)
                        .date(Helper.localDateToDate(currentDate)).build());
                currentDate = nextDate;
            }
            return ResponseEntity.ok(result);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

}

