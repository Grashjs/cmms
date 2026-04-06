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
@Schema(description = "Work order costs and time metrics")
public class WOCostsAndTime {
    @Schema(description = "Total cost")
    private double total;
    
    @Schema(description = "Average cost")
    private double average;
    
    @Schema(description = "Additional costs")
    private double additionalCost;
    
    @Schema(description = "Labor cost")
    private double laborCost;
    
    @Schema(description = "Part cost")
    private double partCost;
    
    @Schema(description = "Labor time")
    private long laborTime;
}
