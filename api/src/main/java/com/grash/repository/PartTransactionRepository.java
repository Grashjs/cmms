package com.grash.repository;

import com.grash.model.PartTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface PartTransactionRepository extends JpaRepository<PartTransaction, Long> {
    Collection<PartTransaction> findByCompany_Id(Long id);

    Collection<PartTransaction> findByWorkOrder_Id(Long id);

    Collection<PartTransaction> findByPart_Id(Long id);

    Collection<PartTransaction> findByWorkOrder_IdAndPart_Id(Long workOrderId, Long partId);

    @Query("""
                SELECT pt
                FROM PartTransaction pt
                WHERE pt.company.id = :id
                  AND pt.createdAt BETWEEN :start AND :end
                  AND pt.quantity > 0
            """)
    Collection<PartTransaction> findConsumptionsByCompanyBetween(
            @Param("id") Long id,
            @Param("start") Date start,
            @Param("end") Date end
    );

    List<PartTransaction> findByWorkOrder_IdIn(List<Long> ids);

    @Query("""
            SELECT pt FROM PartTransaction pt
            LEFT JOIN FETCH pt.part
            LEFT JOIN FETCH pt.workOrder
            WHERE pt.company.id = :companyId
            """)
    Page<PartTransaction> findByCompanyForExport(@Param("companyId") Long companyId, Pageable pageable);

    @Query(value = """
            SELECT a.id, a.name, COALESCE(SUM(p.cost * pc.quantity), 0) AS total_cost
            FROM part_consumption pc
            JOIN work_order wo ON pc.work_order_id = wo.id
            JOIN asset a ON wo.asset_id = a.id
            JOIN part p ON pc.part_id = p.id
            WHERE a.company_id = :companyId
              AND wo.created_at BETWEEN :start AND :end
              AND pc.quantity>0
            GROUP BY a.id, a.name
            ORDER BY total_cost DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopNAssetsByConsumption(@Param("companyId") Long companyId,
                                               @Param("start") Date start,
                                               @Param("end") Date end,
                                               @Param("limit") int limit);
}
