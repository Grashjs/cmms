package com.grash.automation.dto;

import com.grash.automation.action.ActionDescriptor;
import com.grash.automation.event.ChangeType;
import com.grash.automation.event.EntityType;
import com.grash.automation.eval.OperandDescriptor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Everything a rule editor needs to know, in one document: which triggers exist, what can be
 * read as a condition, what can be done as an action, and what may be interpolated into a
 * parameter.
 *
 * <p>This exists so that there is <b>one</b> place where the engine's vocabulary is defined. The
 * old engine defined it four times — {@code WorkflowType}, {@code ConditionType} and
 * {@code ActionType} in Java, mirrored by hand into three TypeScript files — and the copies drifted
 * apart: the settings form offered a condition no evaluator implemented and actions no branch
 * carried out. A rule configured that way saved without complaint and then did nothing. The
 * frontend built on this endpoint cannot drift, because it has no list of its own to drift from.
 */
@Schema(description = "The vocabulary of the automation engine, for building a rule editor")
public record AutomationMetaDTO(
        @Schema(description = "Whether the engine is switched on. When false, rules can be "
                + "configured and saved but no event reaches them")
        boolean engineEnabled,
        List<Trigger> triggers,
        @Schema(description = "Readable operands, including this company's custom fields")
        List<OperandDescriptor> subjects,
        List<ActionDescriptor> actions,
        @Schema(description = "The complete set of ${...} placeholders usable in text parameters")
        List<String> placeholders) {

    /**
     * One trigger, plus the two things a form has to know about it.
     *
     * @param live          whether any service actually publishes this event. A trigger that is
     *                      not live can be stored and will simply never fire — which looks
     *                      exactly like a broken rule, so the editor shows it as unavailable
     *                      rather than hiding or silently offering it.
     * @param changedFields the field names this trigger's diff can report, i.e. the only values
     *                      a "only when this field changed" filter or a {@code CHANGED_TO}
     *                      condition can meaningfully use. Empty means the diff carries nothing.
     */
    @Schema(description = "A trigger: an entity type paired with a kind of change")
    public record Trigger(
            EntityType entityType,
            ChangeType changeType,
            boolean live,
            List<String> changedFields) {
    }
}
