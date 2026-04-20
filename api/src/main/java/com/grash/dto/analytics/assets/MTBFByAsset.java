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
@Schema(description = "Mean time between failures by asset")
public class MTBFByAsset extends AssetMiniDTO {
    @Schema(description = "Mean time between failures")
    private long mtbf;
}
