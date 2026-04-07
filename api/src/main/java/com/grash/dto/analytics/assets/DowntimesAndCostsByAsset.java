package com.grash.dto.analytics.assets;

import com.grash.dto.AssetMiniDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Downtime and costs grouped by asset")
public class DowntimesAndCostsByAsset extends AssetMiniDTO {
    @Schema(description = "Downtime duration")
    private long duration;
    
    @Schema(description = "Work order costs")
    private double workOrdersCosts;
}
