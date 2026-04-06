package com.grash.dto.analytics.parts;

import com.grash.dto.CategoryMiniDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Part consumption grouped by work order category")
public class PartConsumptionByWOCategory extends CategoryMiniDTO {
    @Schema(description = "Total cost")
    private double cost;
}
