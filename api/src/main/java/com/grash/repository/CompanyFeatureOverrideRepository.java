package com.grash.repository;

import com.grash.model.CompanyFeatureOverride;
import com.grash.model.enums.PlanFeatures;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyFeatureOverrideRepository extends JpaRepository<CompanyFeatureOverride, Long> {
    List<CompanyFeatureOverride> findByCompanyId(Long companyId);
    Optional<CompanyFeatureOverride> findByCompanyIdAndFeature(Long companyId, PlanFeatures feature);
    void deleteByCompanyIdAndFeature(Long companyId, PlanFeatures feature);
}
