package com.grash.dto.analytics.workOrders;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Extended mobile work order statistics")
public class MobileWOStatsExtended {
    @Schema(description = "Number of complete work orders")
    private int complete;
    
    @Schema(description = "Number of complete work orders this week")
    private int completeWeek;
    
    @Schema(description = "Compliance rate percentage")
    private double compliantRate;
    
    @Schema(description = "Compliance rate this week percentage")
    private double compliantRateWeek;
}
