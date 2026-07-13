package com.grash.dto.analytics.parts;

import com.grash.dto.AssetMiniDTO;
import com.grash.dto.CategoryMiniDTO;
import com.grash.dto.PartMiniDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartConsumptionByCategory extends CategoryMiniDTO {
    private double cost;
}
