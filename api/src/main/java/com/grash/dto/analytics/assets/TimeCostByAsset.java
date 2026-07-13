package com.grash.dto.analytics.assets;

import com.grash.dto.AssetMiniDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeCostByAsset extends AssetMiniDTO {
    private long time;
    private double cost;
}
