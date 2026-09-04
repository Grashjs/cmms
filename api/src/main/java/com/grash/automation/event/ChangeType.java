package com.grash.automation.event;

/**
 * What happened to an entity. Together with {@link EntityType} this is a rule's trigger:
 * the old engine spelled the same thing out as ten fixed enum values (WORK_ORDER_CREATED,
 * REQUEST_APPROVED, …), which is why adding an entity meant adding a value and a switch.
 */
public enum ChangeType {
    CREATED,
    UPDATED,
    ARCHIVED,
    CLOSED,
    APPROVED,
    REJECTED
}
