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
@Schema(description = "Downtime counts by asset")
public class DowntimesByAsset extends AssetMiniDTO {
    @Schema(description = "Downtime count")
    private int count;
    
    @Schema(description = "Downtime percentage")
    private long percent;
}
