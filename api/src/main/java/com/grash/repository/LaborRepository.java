package com.grash.repository;

import com.grash.model.Labor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface LaborRepository extends JpaRepository<Labor, Long> {
    Collection<Labor> findByWorkOrder_Id(Long id);

    void deleteByCompany_IdAndIsDemoTrue(Long companyId);
}
