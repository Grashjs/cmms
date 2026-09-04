package com.grash.automation.model;

/**
 * The outcome of one rule evaluation. {@link #SKIPPED} is the reason this log exists at all:
 * "why did my rule not fire?" is unanswerable without a record of the runs that decided not to.
 */
public enum RunStatus {
    SUCCESS,
    SKIPPED,
    FAILED
}
