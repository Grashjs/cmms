package com.grash.automation.dto;

import com.grash.automation.model.ActionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "One action of a rule")
public record AutomationActionPostDTO(

        @NotNull
        @Schema(description = "What to do", example = "CREATE_WORK_ORDER")
        ActionType actionType,

        @Schema(description = "JSON object. String values may reference the triggering entity with "
                + "${trigger.id}, ${trigger.asset.id}, ${trigger.asset.name} or "
                + "${trigger.asset.status}. CREATE_WORK_ORDER takes title, priority, category, "
                + "asset; NOTIFY takes message and exactly one of team or user; SET_CUSTOM_FIELD "
                + "takes customField and value.",
                example = "{\"title\":\"Störung ${trigger.asset.name}\",\"priority\":\"HIGH\","
                        + "\"asset\":\"${trigger.asset.id}\"}")
        String parameters,

        @Schema(description = "Execution order, lowest first")
        Integer orderIndex,

        @Schema(description = "Stop the rule when this step fails; defaults to true")
        Boolean abortOnFailure) {
}
