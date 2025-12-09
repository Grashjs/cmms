package com.grash.repository;

import com.grash.model.TimeCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface TimeCategoryRepository extends JpaRepository<TimeCategory, Long> {
    Collection<TimeCategory> findByCompanySettings_Id(Long id);

    Optional<TimeCategory> findByNameIgnoreCaseAndCompanySettings_Id(String name, Long id);

    void deleteByCompanySettings_Company_IdAndIsDemoTrue(Long companyId);
}
