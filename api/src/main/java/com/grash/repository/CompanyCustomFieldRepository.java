package com.grash.repository;

import com.grash.model.CompanyCustomField;
import com.grash.model.CompanySettings;
import com.grash.model.enums.CustomFieldType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompanyCustomFieldRepository extends JpaRepository<CompanyCustomField, Long> {
    Page<CompanyCustomField> findByCompanySettings(CompanySettings companySettings, Pageable pageable);

    List<CompanyCustomField> findByCompanySettings(CompanySettings companySettings);

    Page<CompanyCustomField> findByCompanySettingsAndFieldType(CompanySettings companySettings, CustomFieldType fieldType, Pageable pageable);

    List<CompanyCustomField> findByCompanySettingsAndFieldType(CompanySettings companySettings, CustomFieldType fieldType);
}

