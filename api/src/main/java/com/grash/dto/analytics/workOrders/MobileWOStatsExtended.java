package com.grash.dto.analytics.workOrders;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MobileWOStatsExtended {
    private int complete;
    private int completeWeek;
    private double compliantRate;
    private double compliantRateWeek;
}
