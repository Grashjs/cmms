package com.grash.dto.analytics.parts;

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
@Schema(description = "Part consumption grouped by asset")
public class PartConsumptionsByAsset extends AssetMiniDTO {
    @Schema(description = "Total cost")
    private double cost;
}
