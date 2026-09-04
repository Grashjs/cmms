package com.grash.automation.eval;

import com.grash.automation.model.AutomationCondition;
import com.grash.automation.model.AutomationRule;
import com.grash.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Decides whether a rule's conditions hold. All of them have to: the walking skeleton has one
 * implicit AND group, which is exactly what the old engine could express with its
 * {@code allMatch}. OR groups and nesting come with the condition tree in the next phase, and
 * the flat list is the degenerate case of it.
 *
 * <p>No Spring dependencies beyond the resolver list, and no database access of its own — the
 * resolvers do the reading. That is what makes it testable as a function.
 */
@Component
@RequiredArgsConstructor
public class RuleEvaluator {

    private final List<OperandResolver> resolvers;

    /**
     * @return why the rule did not match, or null when it did. A string rather than a boolean
     * because the run log has to be able to say which condition failed — "condition not met" is
     * the answer that made the old engine impossible to support.
     */
    public String firstUnmetCondition(AutomationRule rule, ExecutionContext context) {
        for (AutomationCondition condition : rule.getConditions()) {
            if (!holds(condition, context)) {
                return describe(condition);
            }
        }
        return null;
    }

    private boolean holds(AutomationCondition condition, ExecutionContext context) {
        Object actual = resolverFor(condition).resolve(condition, context);
        String expected = condition.getExpectedValue();

        return switch (condition.getOperator()) {
            case IS -> Objects.equals(asText(actual), expected);
            case IS_NOT -> !Objects.equals(asText(actual), expected);
            case CONTAINS -> actual != null && expected != null && asText(actual).contains(expected);
            case CHANGED_TO -> changedTo(condition, context, actual, expected);
        };
    }

    /**
     * "Changed to X" needs no snapshot of the old value: the event already says which fields
     * differ, so a field that is in that set and now equals X has changed to X. That is the whole
     * reason the event carries a diff.
     */
    private boolean changedTo(AutomationCondition condition, ExecutionContext context,
                              Object actual, String expected) {
        String field = fieldNameOf(condition.getSubject());
        return context.getEvent().changedFields().contains(field)
                && Objects.equals(asText(actual), expected);
    }

    /** {@code asset.status} names the field {@code status}. */
    private String fieldNameOf(String subject) {
        int lastDot = subject.lastIndexOf('.');
        return lastDot < 0 ? subject : subject.substring(lastDot + 1);
    }

    private String asText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private OperandResolver resolverFor(AutomationCondition condition) {
        return resolvers.stream()
                .filter(resolver -> resolver.supports(condition.getSubject()))
                .findFirst()
                // Loudly, not as a false. A subject nothing can read is a broken rule, and the
                // old engine's habit of answering "false" to that question is defect D7.
                .orElseThrow(() -> new CustomException(
                        "No resolver for condition subject \"" + condition.getSubject() + "\"",
                        HttpStatus.UNPROCESSABLE_ENTITY));
    }

    private String describe(AutomationCondition condition) {
        String field = condition.getCustomField() == null
                ? condition.getSubject()
                : condition.getSubject() + "(" + condition.getCustomField().getId() + ")";
        return field + " " + condition.getOperator() + " " + condition.getExpectedValue();
    }
}
