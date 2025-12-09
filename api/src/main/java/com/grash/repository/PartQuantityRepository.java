package com.grash.repository;

import com.grash.model.PartQuantity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface PartQuantityRepository extends JpaRepository<PartQuantity, Long> {
    Collection<PartQuantity> findByCompany_Id(Long id);

    Collection<PartQuantity> findByWorkOrder_Id(Long id);

    Collection<PartQuantity> findByPart_Id(Long id);

    Collection<PartQuantity> findByPurchaseOrder_Id(Long id);

    void deleteByCompany_IdAndIsDemoTrue(Long companyId);
}
