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
@Schema(description = "Work order estimated vs actual hours")
public class WOHours {
    @Schema(description = "Estimated hours")
    private double estimated;
    
    @Schema(description = "Actual hours")
    private int actual;
}
