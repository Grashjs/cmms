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
@Schema(description = "Work order statistics and metrics")
public class WOStats {
    @Schema(description = "Total work orders")
    private int total;
    
    @Schema(description = "Completed work orders")
    private int complete;
    
    @Schema(description = "Compliant work orders")
    private int compliant;
    
    @Schema(description = "Average cycle time")
    private long avgCycleTime;
    
    @Schema(description = "Mean time to acknowledge")
    private long mtta;
}
