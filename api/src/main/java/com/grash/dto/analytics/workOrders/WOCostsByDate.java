package com.grash.dto.analytics.workOrders;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Work order costs grouped by date")
public class WOCostsByDate {
    @Schema(description = "Part cost")
    private double partCost;
    
    @Schema(description = "Additional costs")
    private double additionalCost;
    
    @Schema(description = "Labor cost")
    private double laborCost;
    
    @Schema(description = "Date")
    private Date date;
}
