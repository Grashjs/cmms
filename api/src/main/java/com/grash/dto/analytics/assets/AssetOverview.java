package com.grash.dto.analytics.assets;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Overview of asset performance metrics")
public class AssetOverview {
    @Schema(description = "Mean time between failures")
    private long mtbf;
    
    @Schema(description = "Mean time to repair")
    private long mttr;
    
    @Schema(description = "Total downtime duration")
    private long downtime;
    
    @Schema(description = "Total uptime duration")
    private long uptime;
    
    @Schema(description = "Total cost associated with the asset")
    private double totalCost;
}
