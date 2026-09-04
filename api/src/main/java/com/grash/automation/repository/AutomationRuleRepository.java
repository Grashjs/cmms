package com.grash.automation.repository;

import com.grash.automation.event.ChangeType;
import com.grash.automation.event.EntityType;
import com.grash.automation.model.AutomationRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AutomationRuleRepository extends JpaRepository<AutomationRule, Long> {

    /**
     * The trigger lookup. {@code EnabledTrue} is part of the query rather than a filter applied
     * afterwards, which is how the old engine got it wrong: it loaded every rule for the trigger
     * and never looked at the flag, so a disabled rule kept running.
     */
    List<AutomationRule> findByCompany_IdAndTriggerChangeTypeAndTriggerEntityTypeAndEnabledTrue(
            Long companyId, ChangeType changeType, EntityType entityType);

    Collection<AutomationRule> findByCompany_Id(Long companyId);
}
