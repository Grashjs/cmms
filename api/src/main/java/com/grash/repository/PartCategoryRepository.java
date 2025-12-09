package com.grash.repository;

import com.grash.model.PartCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface PartCategoryRepository extends JpaRepository<PartCategory, Long> {

    Collection<PartCategory> findByCompanySettings_Id(Long id);

    Optional<PartCategory> findByName(String name);

    Optional<PartCategory> findByNameIgnoreCaseAndCompanySettings_Id(String category, Long companySettingsId);

    void deleteByCompanySettings_Company_IdAndIsDemoTrue(Long companyId);
}
