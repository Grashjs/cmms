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
@Schema(description = "Asset cost analytics")
public class AssetsCosts {
    @Schema(description = "Replacement asset value percentage")
    private double rav;
    
    @Schema(description = "Total work order costs")
    private double totalWOCosts;
    
    @Schema(description = "Total acquisition cost")
    private double totalAcquisitionCost;
}
