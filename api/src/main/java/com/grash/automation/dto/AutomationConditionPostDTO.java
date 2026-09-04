package com.grash.automation.dto;

import com.grash.automation.model.ConditionOperator;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Schema(description = "One condition of a rule")
public record AutomationConditionPostDTO(

        @NotEmpty
        @Schema(description = "Operand path. Native asset fields: asset.status, asset.name, "
                + "asset.category, asset.location, asset.primaryUser. A custom field: asset.cf, "
                + "together with customFieldId.", example = "asset.status")
        String subject,

        @Schema(description = "Required when subject is asset.cf, forbidden otherwise", example = "42")
        Long customFieldId,

        @NotNull
        @Schema(description = "How to compare", example = "CHANGED_TO")
        ConditionOperator operator,

        @Schema(description = "The value to compare against", example = "DOWN")
        String expectedValue) {
}
