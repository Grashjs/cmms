package com.grash.repository;

import com.grash.model.PartConsumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface PartConsumptionRepository extends JpaRepository<PartConsumption, Long> {
    Collection<PartConsumption> findByCompany_Id(Long id);

    Collection<PartConsumption> findByWorkOrder_Id(Long id);

    Collection<PartConsumption> findByPart_Id(Long id);

    Collection<PartConsumption> findByCreatedAtBetweenAndCompany_Id(Date
                                                                            date1, Date date2, Long companyId);


    Collection<PartConsumption> findByWorkOrder_IdAndPart_Id(Long workOrderId, Long partId);

    Collection<PartConsumption> findByCompany_IdAndCreatedAtBetween(Long id, Date start, Date end);

    List<PartConsumption> findByWorkOrder_IdIn(List<Long> ids);

    @Query(value = """
            SELECT a.id, a.name, COALESCE(SUM(p.cost * pc.quantity), 0) AS total_cost
            FROM part_consumption pc
            JOIN work_order wo ON pc.work_order_id = wo.id
            JOIN asset a ON wo.asset_id = a.id
            JOIN part p ON pc.part_id = p.id
            WHERE a.company_id = :companyId
              AND wo.created_at BETWEEN :start AND :end
            GROUP BY a.id, a.name
            ORDER BY total_cost DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopNAssetsByPartConsumption(@Param("companyId") Long companyId,
                                                   @Param("start") Date start,
                                                   @Param("end") Date end,
                                                   @Param("limit") int limit);
}
