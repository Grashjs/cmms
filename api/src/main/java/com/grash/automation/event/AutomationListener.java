package com.grash.automation.event;

import com.grash.automation.AutomationAsyncConfig;
import com.grash.automation.AutomationEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Hands a committed entity change to the rule engine.
 *
 * <p>The three properties that make this a listener rather than a call in a service are the same
 * ones spelled out in {@code RequestTriageListener}, which this copies on purpose:
 *
 * <ul>
 *   <li><b>After commit.</b> The publisher runs inside a transaction, so at the moment it fires
 *       the change is not visible outside it. A rule that loads the entity on another thread
 *       would find the old state, or nothing — intermittently, depending on timing.</li>
 *   <li><b>Off the caller thread</b>, and on {@link AutomationAsyncConfig#EXECUTOR} rather than
 *       the shared pool, so a rule never queues behind a CSV export.</li>
 *   <li><b>Failure is invisible.</b> Whatever happens here, the change that triggered it has
 *       already been committed and must stay committed. The old engine ran inline in the same
 *       transaction, so a failing action took the original operation down with it.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutomationListener {

    private final AutomationEngine engine;

    /**
     * Off switch, default off. The engine runs beside the old one rather than replacing it, so
     * until a company has rules there is nothing to gain from having it on — and a flag that can
     * be flipped from the environment without a deploy is worth the line it costs.
     */
    @Value("${automation.enabled:false}")
    private boolean enabled;

    @Async(AutomationAsyncConfig.EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEntityChanged(EntityChangedEvent event) {
        if (!enabled) {
            return;
        }
        try {
            engine.handle(event);
        } catch (Exception exception) {
            log.warn("Automation failed for {} {}; the change itself is unaffected",
                    event.entityType(), event.entityId(), exception);
        }
    }
}
