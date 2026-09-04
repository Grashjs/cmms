package com.grash.automation;

import com.grash.automation.event.EntityChangedEvent;
import com.grash.automation.model.AutomationRule;
import com.grash.automation.repository.AutomationRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * Picks the rules an event concerns and has each of them run.
 *
 * <p>Deliberately not transactional itself: every rule runs in its own transaction
 * ({@link AutomationRuleRunner}) and every outcome is logged in yet another
 * ({@link AutomationRunService}). One rule failing must leave the others, and the log, alone.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutomationEngine {

    private final AutomationRuleRepository ruleRepository;
    private final AutomationRuleRunner runner;
    private final AutomationRunService runService;

    public void handle(EntityChangedEvent event) {
        for (AutomationRule rule : candidates(event)) {
            RunOutcome outcome;
            try {
                outcome = runner.run(rule.getId(), event);
            } catch (Exception exception) {
                // The rule's own transaction has already rolled back at this point. Recording
                // the failure is the only thing left that can still be done, and it happens in a
                // transaction of its own precisely so that it can.
                log.warn("Automation rule {} failed on {} {}", rule.getId(), event.entityType(),
                        event.entityId(), exception);
                outcome = RunOutcome.failed(rootMessage(exception), 0);
            }
            try {
                runService.record(rule.getId(), event.companyId(), event, outcome);
            } catch (Exception exception) {
                log.error("Could not record automation run for rule {}", rule.getId(), exception);
            }
        }
    }

    /**
     * Rules for this trigger, already narrowed to enabled ones by the query, then narrowed again
     * by the changed-field filter. A rule that only cares about {@code status} is not woken by a
     * change to something else — which is an optimisation and the cheapest of the loop guards at
     * the same time.
     */
    @Transactional(readOnly = true)
    protected List<AutomationRule> candidates(EntityChangedEvent event) {
        if (event.companyId() == null) {
            log.warn("Automation event without a company, ignoring: {}", event);
            return Collections.emptyList();
        }
        return ruleRepository
                .findByCompany_IdAndTriggerChangeTypeAndTriggerEntityTypeAndEnabledTrue(
                        event.companyId(), event.changeType(), event.entityType())
                .stream()
                .filter(rule -> matchesChangedFields(rule, event))
                .toList();
    }

    private boolean matchesChangedFields(AutomationRule rule, EntityChangedEvent event) {
        if (rule.getTriggerChangedFields().isEmpty()) {
            return true;
        }
        return rule.getTriggerChangedFields().stream().anyMatch(event.changedFields()::contains);
    }

    /** The innermost message, because "could not commit" tells a rule author nothing. */
    private String rootMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage();
    }
}
