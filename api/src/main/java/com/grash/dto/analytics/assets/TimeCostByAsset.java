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
@Schema(description = "Time and cost metrics by asset")
public class TimeCostByAsset extends AssetMiniDTO {
    @Schema(description = "Time spent")
    private long time;
    
    @Schema(description = "Cost incurred")
    private double cost;
}
