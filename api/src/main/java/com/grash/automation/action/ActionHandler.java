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
     * @throws RuntimeException to fail the step. The engine catches it, records the run as
     *                          FAILED with the message, and either stops or continues depending
     *                          on the step's {@code abortOnFailure}. Throwing is the right
     *                          response to a licence limit or a missing reference — swallowing
     *                          it would reproduce the defect this engine exists to fix.
     */
    void execute(AutomationActionStep step, ExecutionContext context);
}
