package com.grash.automation;

import com.grash.automation.event.EntityChangedEvent;
import com.grash.automation.model.AutomationRun;
import com.grash.automation.repository.AutomationRuleRepository;
import com.grash.automation.repository.AutomationRunRepository;
import com.grash.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes and reads the run log.
 *
 * <p>{@code REQUIRES_NEW} on the write is the point of this class existing separately. A rule
 * whose action throws has its transaction rolled back, and if the log row were written in that
 * same transaction it would roll back with it — the engine would record every run except the ones
 * worth recording.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutomationRunService {

    private final AutomationRunRepository automationRunRepository;
    private final AutomationRuleRepository automationRuleRepository;
    private final CompanyRepository companyRepository;

    /**
     * Takes ids rather than entities: the caller loaded its rules outside any transaction, and
     * handing a detached instance into a fresh one is how you get a surprise select or a stale
     * write. References resolve to the same foreign keys without either.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long ruleId, Long companyId, EntityChangedEvent event, RunOutcome outcome) {
        AutomationRun run = new AutomationRun();
        // By hand, because there is no security context on this thread and
        // CompanyAudit.beforePersist would leave it null against a not-null column.
        run.setCompany(companyRepository.getReferenceById(companyId));
        run.setRule(automationRuleRepository.getReferenceById(ruleId));
        run.setEntityType(event.entityType());
        run.setEntityId(event.entityId());
        run.setStatus(outcome.status());
        run.setDetail(outcome.detail());
        run.setActionsExecuted(outcome.actionsExecuted());
        run.setCorrelationId(event.correlationId().toString());
        run.setDepth(event.depth());
        automationRunRepository.save(run);
    }

    @Transactional(readOnly = true)
    public boolean alreadyRanInThisCascade(Long ruleId, EntityChangedEvent event) {
        return automationRunRepository.existsByRule_IdAndEntityIdAndCorrelationId(
                ruleId, event.entityId(), event.correlationId().toString());
    }

    @Transactional(readOnly = true)
    public Page<AutomationRun> findByRule(Long ruleId, Pageable pageable) {
        return automationRunRepository.findByRule_IdOrderByIdDesc(ruleId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AutomationRun> findByCompany(Long companyId, Pageable pageable) {
        return automationRunRepository.findByCompany_IdOrderByIdDesc(companyId, pageable);
    }
}
