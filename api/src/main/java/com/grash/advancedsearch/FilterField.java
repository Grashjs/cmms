package com.grash.advancedsearch;

import com.grash.model.enums.EnumName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.criteria.JoinType;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "A single filter condition applied to a search query")
public class FilterField {
    @Schema(description = "The field name to filter on")
    private String field;
    @Schema(description = "The type of join to use for related entities")
    private JoinType joinType;
    @Schema(description = "The value to compare against for the filter")
    private Object value;
    @Schema(description = "The operation to perform (e.g., 'eq', 'contains', 'gt', 'lt')")
    private String operation;
    @Schema(description = "List of values for multi-value filter operations")
    private List<Object> values = new ArrayList<>();
    @Schema(description = "Alternative filter conditions (OR logic)")
    private List<FilterField> alternatives;
    @Schema(description = "Enum name for fields that filter on enum values")
    private EnumName enumName;
}

