package com.grash.repository;

import com.grash.model.WorkOrderCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface WorkOrderCategoryRepository extends JpaRepository<WorkOrderCategory, Long> {
    Collection<WorkOrderCategory> findByCompanySettings_Id(Long id);

    Optional<WorkOrderCategory> findByNameIgnoreCaseAndCompanySettings_Id(String name, Long id);

    void deleteByCompanySettings_Company_IdAndIsDemoTrue(Long companyId);
}
