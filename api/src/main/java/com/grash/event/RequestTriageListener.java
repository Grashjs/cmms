package com.grash.event;

import com.grash.service.RequestQualificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Triages a request shortly after it was created.
 *
 * <p>Three properties of this listener are the whole reason it exists as a listener rather than as
 * a few lines in {@code RequestController}:
 *
 * <ul>
 *   <li><b>After commit.</b> {@code RequestController} is {@code @Transactional}, so at the moment
 *       the request is created it is not yet visible outside that transaction. A job started there
 *       and running on another thread would look for a row that is not there yet and find nothing
 *       - intermittently, depending on timing, which is the worst way for this to fail.
 *       {@code AFTER_COMMIT} is what removes that race.</li>
 *   <li><b>Off the caller thread.</b> The person submitting a request through the portal must not
 *       wait for triage, now or when a later stage puts a language model behind it. The
 *       {@code @Async} executor is already configured for the export and mail paths.</li>
 *   <li><b>Failure is invisible.</b> Anything thrown here is caught and logged. If triage breaks,
 *       the request is still created, the notifications still go out, and the admin triages by
 *       hand exactly as before. This feature is an assistant; it is never allowed to be the reason
 *       a request does not arrive.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequestTriageListener {

    private final RequestQualificationService requestQualificationService;

    /**
     * Off switch. Triage runs on every incoming request and touches the whole asset list of a
     * company; being able to turn it off from the environment, without a deploy, is worth the one
     * line it costs.
     */
    @Value("${triage.enabled:true}")
    private boolean enabled;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRequestCreated(RequestCreatedEvent event) {
        if (!enabled) return;
        try {
            requestQualificationService.qualify(event.requestId());
        } catch (Exception exception) {
            log.warn("Triage failed for request {}; it stays untriaged and nothing else is affected",
                    event.requestId(), exception);
        }
    }
}
