package com.grash.automation.event;

/**
 * Which kind of entity changed. Only {@link #ASSET} is published so far — the walking
 * skeleton deliberately wires one trigger end to end rather than all of them shallowly.
 * The remaining values exist because the rule table stores them and the metadata endpoint
 * will offer them as they are wired; an unpublished type simply never matches a rule.
 */
public enum EntityType {
    ASSET,
    WORK_ORDER,
    REQUEST,
    PURCHASE_ORDER,
    PART,
    TASK
}
