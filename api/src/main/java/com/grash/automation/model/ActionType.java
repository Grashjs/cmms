package com.grash.automation.model;

/**
 * What a rule can do. As with {@link ConditionOperator}, this lists only what a handler
 * actually carries out — the old engine offered five actions that were {@code //TODO} stubs,
 * which produced rules that saved cleanly and then did nothing.
 */
public enum ActionType {
    /** Create a work order, optionally for the entity that triggered the rule. */
    CREATE_WORK_ORDER,
    /** In-app notification to the members of a team or to named users. */
    NOTIFY,
    /** Write a custom field value on the triggering asset. */
    SET_CUSTOM_FIELD
}
