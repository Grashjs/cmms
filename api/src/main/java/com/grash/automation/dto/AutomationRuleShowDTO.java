package com.grash.automation.dto;

import com.grash.automation.event.ChangeType;
import com.grash.automation.event.EntityType;
import com.grash.automation.model.ActionType;
import com.grash.automation.model.AutomationRule;
import com.grash.automation.model.ConditionOperator;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Set;

/**
 * A rule on the way out. Written by hand rather than serialising the entity: the children hold a
 * back-reference to their rule, and Jackson would follow it straight into a cycle.
 */
@Schema(description = "An automation rule")
public record AutomationRuleShowDTO(
        Long id,
        String title,
        ChangeType triggerChangeType,
        EntityType triggerEntityType,
        Set<String> triggerChangedFields,
        boolean enabled,
        Integer maxDepth,
        List<Condition> conditions,
        List<Action> actions) {

    @Schema(description = "One condition of a rule")
    public record Condition(Long id, String subject, Long customFieldId, String customFieldLabel,
                            ConditionOperator operator, String expectedValue) {
    }

    @Schema(description = "One action of a rule")
    public record Action(Long id, ActionType actionType, String parameters, int orderIndex,
                         boolean abortOnFailure) {
    }

    public static AutomationRuleShowDTO of(AutomationRule rule) {
        return new AutomationRuleShowDTO(
                rule.getId(),
                rule.getTitle(),
                rule.getTriggerChangeType(),
                rule.getTriggerEntityType(),
                rule.getTriggerChangedFields(),
                rule.isEnabled(),
                rule.getMaxDepth(),
                rule.getConditions().stream()
                        .map(condition -> new Condition(
                                condition.getId(),
                                condition.getSubject(),
                                condition.getCustomField() == null ? null : condition.getCustomField().getId(),
                                // The label travels with the id so a rule list is readable without
                                // a second round trip per condition.
                                condition.getCustomField() == null ? null : condition.getCustomField().getLabel(),
                                condition.getOperator(),
                                condition.getExpectedValue()))
                        .toList(),
                rule.getActions().stream()
                        .map(action -> new Action(
                                action.getId(),
                                action.getActionType(),
                                action.getParameters(),
                                action.getOrderIndex(),
                                action.isAbortOnFailure()))
                        .toList());
    }
}
