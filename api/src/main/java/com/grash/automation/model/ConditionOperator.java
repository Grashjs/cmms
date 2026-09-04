package com.grash.automation.model;

/**
 * How a condition compares its operand to its value.
 *
 * <p>This enum lists exactly what the evaluator implements, and nothing else. The old engine did
 * the opposite: {@code TITLE_CONTAINS} sat in its enums and in its settings form for years while
 * no switch handled it, so it fell through to {@code default: return false} and silently
 * disabled every rule that used it. An operator that exists here is one that works.
 */
public enum ConditionOperator {
    /** Equal, compared as text after both sides are stringified. */
    IS,
    IS_NOT,
    /** Case-sensitive substring. */
    CONTAINS,
    /**
     * The operand changed <em>to</em> this value in the change that triggered the rule. Needs
     * the event's field diff, so it only means anything for {@link com.grash.automation.event.ChangeType#UPDATED}.
     */
    CHANGED_TO
}
