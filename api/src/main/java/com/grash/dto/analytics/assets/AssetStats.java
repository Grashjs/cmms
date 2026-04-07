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
@Schema(description = "Asset statistics and metrics")
public class AssetStats {
    @Schema(description = "Total downtime duration")
    private long downtime;
    
    @Schema(description = "Asset availability percentage")
    private long availability;
    
    @Schema(description = "Number of downtime events")
    private int downtimeEvents;
}
