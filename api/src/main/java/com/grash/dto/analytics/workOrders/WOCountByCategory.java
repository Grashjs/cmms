package com.grash.dto.analytics.workOrders;

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
@Schema(description = "Work order count grouped by category")
public class WOCountByCategory extends CategoryMiniDTO {
    @Schema(description = "Work order count")
    private int count;
}
