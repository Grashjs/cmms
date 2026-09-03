package com.grash.repository;

import com.grash.model.Workflow;
import com.grash.model.enums.workflow.WFMainCondition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface WorkflowRepository extends JpaRepository<Workflow, Long> {
    Collection<Workflow> findByCompany_Id(Long id);

    /**
     * The trigger lookup. Filtering on {@code enabled} here rather than at the call sites is
     * deliberate: there are ten of them, and every one of them used to run disabled rules.
     */
    Collection<Workflow> findByMainConditionAndCompany_IdAndEnabledTrue(WFMainCondition mainCondition,
                                                                        Long companyId);
}
