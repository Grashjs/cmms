package com.grash.automation.action;

import com.grash.automation.eval.ExecutionContext;
import com.grash.automation.model.ActionType;
import com.grash.automation.model.AutomationActionStep;

/**
 * Carries out one action. One implementation per {@link ActionType}, registered by Spring and
 * looked up by type — instead of the central switch the old engine kept in
 * {@code WorkflowService}, where five of the branches were {@code //TODO}.
 *
 * <p>Every handler that persists something has one obligation that is easy to miss and fatal to
 * skip: <b>set the company explicitly</b>. {@code CompanyAudit.beforePersist} reads it off the
 * security context, and there is none on the executor thread, so the insert would fail on a
 * not-null constraint. {@code RequestQualificationService} has the same note for the same reason.
 */
public interface ActionHandler {

    ActionType getType();

    /**
     * What this action needs, so the editor can build the form for it.
     *
     * <p>The counterpart of {@link com.grash.automation.eval.OperandResolver#describe}: the
     * frontend asks the server what exists instead of keeping its own list. The old engine kept
     * four such lists — an enum in Java and three mirrors in TypeScript — and they disagreed:
     * the settings form offered actions no branch implemented, and offered them for triggers
     * where they were never wired at all. A handler that exists here can be configured, and one
     * that does not exist cannot be, without anyone maintaining a second list.
     */
    ActionDescriptor descriptor();

    /**
     * @throws RuntimeException to fail the step. The engine catches it, records the run as
     *                          FAILED with the message, and either stops or continues depending
     *                          on the step's {@code abortOnFailure}. Throwing is the right
     *                          response to a licence limit or a missing reference — swallowing
     *                          it would reproduce the defect this engine exists to fix.
     */
    void execute(AutomationActionStep step, ExecutionContext context);
}
