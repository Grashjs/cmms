package com.grash.repository;

import com.grash.model.AdditionalCost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface AdditionalCostRepository extends JpaRepository<AdditionalCost, Long> {
    Collection<AdditionalCost> findByWorkOrder_Id(Long id);

    void deleteByWorkOrder_Company_IdAndIsDemoTrue(Long companyId);

}
