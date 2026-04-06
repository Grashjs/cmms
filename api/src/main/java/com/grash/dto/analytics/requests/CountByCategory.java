package com.grash.dto.analytics.requests;

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
@Schema(description = "Request count grouped by category")
public class CountByCategory extends CategoryMiniDTO {
    @Schema(description = "Request count")
    private int count;
}
