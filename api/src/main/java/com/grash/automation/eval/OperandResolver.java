package com.grash.automation.eval;

import com.grash.automation.model.AutomationCondition;

/**
 * Reads the value a condition compares against.
 *
 * <p>This is the extension point that makes new kinds of conditions cheap. The old engine needed
 * an enum value, a case in a switch inside the JPA entity, and the same value mirrored into three
 * TypeScript files. Here a new source of conditions is one more implementation of this interface,
 * registered by Spring, and the evaluator does not change.
 *
 * <p>A {@code descriptor()} method belongs here too — it is what lets the metadata endpoint tell
 * the frontend which subjects exist, what type they are and which values they can take. It is
 * deliberately not in the walking skeleton, where rules are configured through the API.
 */
public interface OperandResolver {

    /** Whether this resolver handles the condition's subject path. */
    boolean supports(String subject);

    /**
     * The current value, or null when the entity has none. Null is a legitimate answer, not an
     * error: an asset simply may not have a value for a custom field.
     */
    Object resolve(AutomationCondition condition, ExecutionContext context);
}
