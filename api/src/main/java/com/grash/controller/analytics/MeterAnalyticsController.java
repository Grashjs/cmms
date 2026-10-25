package com.grash.controller.analytics;

import com.grash.dto.DateRange;
import com.grash.dto.analytics.meters.MeterAnalytics;
import com.grash.dto.analytics.meters.MeterOption;
import com.grash.exception.CustomException;
import com.grash.model.User;
import com.grash.security.CurrentUser;
import com.grash.service.MeterAnalyticsService;
import com.grash.utils.Helper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/analytics/meters")
@Tag(name = "Meter Analytics", description = "Trend, thresholds and consumption of the meters")
@RequiredArgsConstructor
public class MeterAnalyticsController {

    private final MeterAnalyticsService meterAnalyticsService;

    @GetMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Operation(summary = "Meters of the company, to pick one in the meter analytics")
    public List<MeterOption> getMeters(@Parameter(hidden = true) @CurrentUser User user,
                                       @RequestParam(required = false) @Parameter(description = "Filter by specific " +
                                               "company") Long companyId) {
        checkAccess(user);
        return meterAnalyticsService.getMeters(Helper.resolveCompanyId(user, companyId));
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getMeterAnalytics",
            key = "@analyticsCacheService.key(#user, #companyId, #dateRange) + '_m' + #id"
    )
    @Operation(summary = "Readings of a meter over the period: minimum, average and maximum per hour, day, week or " +
            "month, readings outside the limits of its triggers and, for cumulative meters, the consumption per " +
            "period and between failures of the asset")
    public MeterAnalytics getAnalytics(@Parameter(hidden = true) @CurrentUser User user,
                                       @Parameter(description = "Meter ID") @PathVariable("id") Long id,
                                       @Parameter(description = "Date range") @RequestBody DateRange dateRange,
                                       @RequestParam(required = false) @Parameter(description = "Filter by specific " +
                                               "company") Long companyId) {
        checkAccess(user);
        return meterAnalyticsService.getAnalytics(Helper.resolveCompanyId(user, companyId), id,
                dateRange.getStart(), dateRange.getEnd());
    }

    private static void checkAccess(User user) {
        if (!user.canSeeAnalytics()) throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }
}
