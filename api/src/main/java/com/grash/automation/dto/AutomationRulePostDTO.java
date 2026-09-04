package com.grash.automation.dto;

import com.grash.automation.event.ChangeType;
import com.grash.automation.event.EntityType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Set;

/**
 * A rule as it comes in over the API. Rules are configured through Swagger in the walking
 * skeleton; the metadata-driven editor comes with the next phase, and it will post this shape.
 */
@Schema(description = "An automation rule to create or replace")
public record AutomationRulePostDTO(

        @NotEmpty
        @Schema(description = "Name of the rule", example = "Kritische Anlage fällt aus")
        String title,

        @NotNull
        @Schema(description = "Which kind of change fires the rule", example = "UPDATED")
        ChangeType triggerChangeType,

        @NotNull
        @Schema(description = "Which kind of entity the change is about", example = "ASSET")
        EntityType triggerEntityType,

        @Schema(description = "Only fire when one of these fields changed; empty means any change",
                example = "[\"status\"]")
        Set<String> triggerChangedFields,

        @Schema(description = "Whether the rule runs; defaults to true when omitted")
        Boolean enabled,

        @Schema(description = "Cascade limit for this rule; omit to use the engine default")
        Integer maxDepth,

        @Valid
        @Schema(description = "All of these have to hold. Empty means the rule always fires.")
        List<AutomationConditionPostDTO> conditions,

        @Valid
        @NotEmpty
        @Schema(description = "Executed in order")
        List<AutomationActionPostDTO> actions) {
}
