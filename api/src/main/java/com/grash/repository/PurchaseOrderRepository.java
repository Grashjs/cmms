package com.grash.repository;

import com.grash.model.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long>, JpaSpecificationExecutor<PurchaseOrder> {
    Collection<PurchaseOrder> findByCompany_Id(@Param("x") Long id);

    /**
     * Purchase orders raised for a given work order. A finder rather than a @OneToMany on
     * WorkOrder: the work order DTOs are already heavy, and an extra managed collection
     * there would be serialised on every work-order read for a link that is only shown on
     * the detail page.
     */
    Collection<PurchaseOrder> findByWorkOrder_Id(Long workOrderId);
}
