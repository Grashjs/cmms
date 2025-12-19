package com.grash.repository;

import com.grash.model.AssetCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface AssetCategoryRepository extends JpaRepository<AssetCategory, Long> {

    Collection<AssetCategory> findByCompanySettings_Id(Long id);

    Optional<AssetCategory> findByName(String name);

    Optional<AssetCategory> findByNameIgnoreCaseAndCompanySettings_Id(String category, Long companySettingsId);

    void deleteByCompanySettings_Company_IdAndIsDemoTrue(Long companyId);
}
