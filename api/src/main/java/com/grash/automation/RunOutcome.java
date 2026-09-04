package com.grash.automation;

import com.grash.automation.model.RunStatus;

/**
 * What one rule evaluation produced. {@code detail} is always filled for anything other than a
 * plain success — a skipped run without a reason is as useless as no log at all.
 */
public record RunOutcome(RunStatus status, String detail, int actionsExecuted) {

    public static RunOutcome success(int actionsExecuted) {
        return new RunOutcome(RunStatus.SUCCESS, null, actionsExecuted);
    }

    public static RunOutcome skipped(String reason) {
        return new RunOutcome(RunStatus.SKIPPED, reason, 0);
    }

    public static RunOutcome failed(String error, int actionsExecuted) {
        return new RunOutcome(RunStatus.FAILED, error, actionsExecuted);
    }
}
