package com.grash.repository;

import com.grash.model.PurchaseOrderCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface PurchaseOrderCategoryRepository extends JpaRepository<PurchaseOrderCategory, Long> {
    Collection<PurchaseOrderCategory> findByCompanySettings_Id(Long id);

    Optional<PurchaseOrderCategory> findByNameIgnoreCaseAndCompanySettings_Id(String name, Long id);

    void deleteByCompanySettings_Company_IdAndIsDemoTrue(Long companyId);

}
