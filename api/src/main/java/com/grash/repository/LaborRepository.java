package com.grash.repository;

import com.grash.model.Labor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface LaborRepository extends JpaRepository<Labor, Long> {
    Collection<Labor> findByWorkOrder_Id(Long id);

    Optional<Labor> findByIdAndCompany_IdAndWorkOrder_Id(Long id, Long companyId, Long workOrderId);

    boolean existsByIdAndCompany_IdAndWorkOrder_Id(Long id, Long companyId, Long workOrderId);

    void deleteByCompany_IdAndIsDemoTrue(Long companyId);
}
