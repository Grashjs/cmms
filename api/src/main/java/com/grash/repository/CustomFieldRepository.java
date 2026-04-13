package com.grash.repository;

import com.grash.model.CustomField;
import com.grash.model.CompanySettings;
import com.grash.model.enums.CustomFieldType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomFieldRepository extends JpaRepository<CustomField, Long> {
    Page<CustomField> findByCompanySettings(CompanySettings companySettings, Pageable pageable);

    List<CustomField> findByCompanySettings(CompanySettings companySettings);

    Page<CustomField> findByCompanySettingsAndFieldType(CompanySettings companySettings, CustomFieldType fieldType,
                                                        Pageable pageable);

    List<CustomField> findByCompanySettingsAndFieldType(CompanySettings companySettings, CustomFieldType fieldType);
}

