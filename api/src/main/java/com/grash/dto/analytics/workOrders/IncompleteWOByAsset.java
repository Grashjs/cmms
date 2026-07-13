package com.grash.dto.analytics.workOrders;

import com.grash.dto.AssetMiniDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncompleteWOByAsset extends AssetMiniDTO {
    private int count;
    private long averageAge; //days
}
