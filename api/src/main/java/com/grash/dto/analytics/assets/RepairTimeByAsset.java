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
@Schema(description = "Repair time by asset")
public class RepairTimeByAsset extends AssetMiniDTO {
    @Schema(description = "Repair duration in days")
    private long duration;
}
