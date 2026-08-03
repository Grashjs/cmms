package com.grash.repository;

import com.grash.model.CustomField;
import com.grash.model.CompanySettings;
import com.grash.model.enums.CustomFieldEntityType;
import com.grash.model.enums.CustomFieldType;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomFieldRepository extends JpaRepository<CustomField, Long> {
    Page<CustomField> findByCompanySettings(CompanySettings companySettings, Pageable pageable);

    List<CustomField> findByCompanySettings(CompanySettings companySettings);

    /**
     * Same as {@link #findByCompanySettings(CompanySettings)} but pre-loads the asset
     * category binding. Callers map every field to a DTO that exposes the categories, so
     * without the fetch join each field would trigger its own query.
     */
    @Query("select distinct cf from CustomField cf left join fetch cf.assetCategories "
            + "where cf.companySettings = :companySettings")
    List<CustomField> findByCompanySettingsFetchAssetCategories(
            @Param("companySettings") CompanySettings companySettings);

    List<CustomField> findByCompanySettingsAndEntityType(CompanySettings companySettings,
                                                         CustomFieldEntityType entityType);

    @Query("select distinct cf from CustomField cf left join fetch cf.assetCategories "
            + "where cf.companySettings = :companySettings and cf.entityType = :entityType")
    List<CustomField> findByCompanySettingsAndEntityTypeFetchAssetCategories(
            @Param("companySettings") CompanySettings companySettings,
            @Param("entityType") CustomFieldEntityType entityType);

    int countByCompanySettings_IdAndEntityType(Long id, @NotNull CustomFieldEntityType entityType);
}

