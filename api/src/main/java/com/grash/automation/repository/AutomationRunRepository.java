package com.grash.automation.repository;

import com.grash.automation.model.AutomationRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutomationRunRepository extends JpaRepository<AutomationRun, Long> {

    Page<AutomationRun> findByRule_IdOrderByIdDesc(Long ruleId, Pageable pageable);

    Page<AutomationRun> findByCompany_IdOrderByIdDesc(Long companyId, Pageable pageable);

    /**
     * Loop guard. The same rule, on the same entity, within the same cascade, is a rule feeding
     * itself — the classic case being an ASSET_UPDATED rule whose action writes that asset.
     */
    boolean existsByRule_IdAndEntityIdAndCorrelationId(Long ruleId, Long entityId, String correlationId);
}
