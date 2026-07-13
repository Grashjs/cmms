package com.grash.dto.analytics.parts;

import com.grash.dto.CategoryMiniDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartConsumptionByWOCategory extends CategoryMiniDTO {
    private double cost;
}
