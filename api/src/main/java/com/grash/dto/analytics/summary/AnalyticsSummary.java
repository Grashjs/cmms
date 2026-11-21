package com.grash.dto.analytics.summary;

import com.grash.dto.analytics.assets.AssetAvailability;
import com.grash.dto.analytics.requests.RequestStats;
import com.grash.dto.analytics.workOrders.MaintenanceKpis;
import com.grash.dto.analytics.workOrders.WOCostsAndTime;
import com.grash.dto.analytics.workOrders.WOStats;

import java.util.Date;
import java.util.List;

/**
 * Values of the maintenance summary report for a period and for the previous period of the same length.
 *
 * @param lowestAvailability assets with the lowest availability in the period, worst first
 */
public record AnalyticsSummary(Period current, Period previous, List<AssetAvailability> lowestAvailability) {

    public record Period(Date start,
                         Date end,
                         WOStats workOrders,
                         MaintenanceKpis kpis,
                         WOCostsAndTime costs,
                         RequestStats requests) {
    }
}
