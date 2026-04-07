package com.grash.dto.analytics.parts;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Part statistics and metrics")
public class PartStats {
    @Schema(description = "Total consumption cost")
    private double totalConsumptionCost;
    
    @Schema(description = "Number of consumed parts")
    private int consumedCount;
}
