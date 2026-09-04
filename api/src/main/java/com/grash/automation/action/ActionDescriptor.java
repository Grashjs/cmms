package com.grash.automation.action;

import com.grash.automation.model.ActionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * What an action needs, described well enough for a UI to build the form for it.
 *
 * <p>The counterpart to {@link com.grash.automation.eval.OperandDescriptor}: a new handler brings
 * its own parameter list, so it appears in the editor with the right inputs and nothing in the
 * frontend has to learn about it.
 */
@Schema(description = "An action a rule can perform, and the parameters it takes")
public record ActionDescriptor(
        ActionType type,
        String labelKey,
        List<Parameter> parameters) {

    /**
     * @param valueType   TEXT, NUMBER, ENUM, ENTITY_WORK_ORDER_CATEGORY, ENTITY_TEAM,
     *                    ENTITY_USER, ENTITY_CUSTOM_FIELD — the editor picks the input from this
     * @param options     for ENUM, the permitted values
     * @param placeholders whether {@code ${trigger.…}} may be used in this parameter. Only true
     *                    for free text: substituting an asset id into a title is the point of the
     *                    mechanism, substituting one into a category reference is a mistake the
     *                    editor should not invite.
     */
    @Schema(description = "One parameter of an action")
    public record Parameter(
            String name,
            String labelKey,
            String valueType,
            boolean required,
            List<String> options,
            boolean placeholders) {

        public static Parameter text(String name, boolean required) {
            return new Parameter(name, "automation_param_" + name, "TEXT", required, List.of(), true);
        }

        public static Parameter enumOf(String name, boolean required, List<String> options) {
            return new Parameter(name, "automation_param_" + name, "ENUM", required, options, false);
        }

        public static Parameter entity(String name, String entityType, boolean required) {
            return new Parameter(name, "automation_param_" + name, "ENTITY_" + entityType, required,
                    List.of(), false);
        }

        /**
         * A reference to the entity that triggered the rule. Rendered as a fixed choice rather
         * than a picker — "this asset" is the answer nearly every rule wants, and typing an id
         * there by hand is how a rule ends up pointing at the wrong machine.
         */
        public static Parameter triggerReference(String name, boolean required) {
            return new Parameter(name, "automation_param_" + name, "TRIGGER_REFERENCE", required,
                    List.of("${trigger.asset.id}"), true);
        }
    }
}
