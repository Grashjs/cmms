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
@Schema(description = "Incomplete work order statistics")
public class WOIncompleteStats {
    @Schema(description = "Total incomplete count")
    private int total;
    
    @Schema(description = "Average age in days")
    private int averageAge;
}
