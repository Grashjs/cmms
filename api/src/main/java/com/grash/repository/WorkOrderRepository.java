package com.grash.repository;

import com.grash.model.WorkOrder;
import com.grash.model.enums.Priority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long>, JpaSpecificationExecutor<WorkOrder> {
    Collection<WorkOrder> findByCompany_Id(Long id);

    @Query("SELECT w FROM WorkOrder w " +
            "LEFT JOIN FETCH w.asset " +
            "LEFT JOIN FETCH w.location " +
            "LEFT JOIN FETCH w.assignedTo " +
            "LEFT JOIN FETCH w.completedBy " +
            "LEFT JOIN FETCH w.parentPreventiveMaintenance " +
            "WHERE w.company.id = :companyId")
    Page<WorkOrder> findByCompanyForExport(@Param("companyId") Long companyId, Pageable pageable);

    Collection<WorkOrder> findByIdInAndCompany_Id(List<Long> ids, Long companyId);

    Collection<WorkOrder> findByAsset_Id(Long id);

    Collection<WorkOrder> findByLocation_Id(Long id);

    Page<WorkOrder> findByParentPreventiveMaintenance_Id(Long id, Pageable pageable);

    Collection<WorkOrder> findByPrimaryUser_Id(Long id);

    Collection<WorkOrder> findByCompletedBy_Id(Long id);

    Collection<WorkOrder> findByPriorityAndCompany_Id(Priority priority, Long companyId);

    Collection<WorkOrder> findByCategory_Id(Long id);

    Collection<WorkOrder> findByCompletedOnBetweenAndCompany_Id(Date date1, Date date2, Long id);

    Collection<WorkOrder> findByCreatedBy(Long id);

    Collection<WorkOrder> findByDueDateBetweenAndCompany_Id(Date date1, Date date2, Long id);

    Optional<WorkOrder> findByIdAndCompany_Id(Long id, Long companyId);

    Collection<WorkOrder> findByCreatedByAndCreatedAtBetween(Long id, Date date1, Date date2);

    Collection<WorkOrder> findByCompletedBy_IdAndCreatedAtBetween(Long id, Date date1, Date date2);

    @Query("SELECT DISTINCT wo FROM WorkOrder wo " +
            "LEFT JOIN wo.assignedTo assigned " +
            "LEFT JOIN wo.team team " +
            "WHERE wo.primaryUser.id = :id " +
            "OR assigned.id = :id " +
            "OR :id IN (SELECT user.id FROM team.users user)")
    Collection<WorkOrder> findByAssignedToUser(@Param("id") Long id);

    @Query("SELECT DISTINCT wo FROM WorkOrder wo " +
            "LEFT JOIN wo.assignedTo assigned " +
            "LEFT JOIN wo.team team " +
            "WHERE (wo.primaryUser.id = :id " +
            "OR assigned.id = :id " +
            "OR :id IN (SELECT user.id FROM team.users user)) AND wo.createdAt between :start and :end")
    Collection<WorkOrder> findByAssignedToUserAndCreatedAtBetween(@Param("id") Long id, @Param("start") Date start,
                                                                  @Param("end") Date end);

    Collection<WorkOrder> findByAsset_IdAndCreatedAtBetween(Long id, Date start, Date end);

    Collection<WorkOrder> findByCompany_IdAndCreatedAtBetween(Long id, Date start, Date end);

    Collection<WorkOrder> findByPriorityAndCompany_IdAndCreatedAtBetween(Priority priority, Long companyId,
                                                                         Date start, Date end);

    Collection<WorkOrder> findByCategory_IdAndCreatedAtBetween(Long id, Date start, Date end);

    void deleteByCompany_IdAndIsDemoTrue(Long companyId);

    @Query(value = """
            SELECT a.id, a.name, COUNT(wo.id) AS cnt,
                   COALESCE(AVG(EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - wo.created_at)) / 86400), 0) AS avg_age
            FROM work_order wo
            INNER JOIN asset a ON wo.asset_id = a.id
            WHERE wo.company_id = :companyId
              AND wo.created_at BETWEEN :start AND :end
              AND wo.status != 3
            GROUP BY a.id, a.name
            ORDER BY cnt DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopNAssetsByIncompleteWO(@Param("companyId") Long companyId,
                                                @Param("start") Date start,
                                                @Param("end") Date end,
                                                @Param("limit") int limit);

    @Query(value = """
            WITH wo_agg AS (
              SELECT
                wo.asset_id,
                COALESCE(labor_stats.total_labor_time, 0) AS labor_time,
                COALESCE(labor_stats.total_labor_cost, 0) AS labor_cost,
                COALESCE(part_stats.total_part_cost, 0) AS part_cost,
                COALESCE(add_stats.total_additional_cost, 0) AS additional_cost
              FROM work_order wo
              LEFT JOIN (
                SELECT l.work_order_id,
                       SUM(l.duration) AS total_labor_time,
                       SUM(l.hourly_rate * l.duration / 3600) AS total_labor_cost
                FROM labor l
                GROUP BY l.work_order_id
              ) labor_stats ON labor_stats.work_order_id = wo.id
              LEFT JOIN (
                SELECT pq.work_order_id, SUM(p.cost * pq.quantity) AS total_part_cost
                FROM part_quantity pq
                JOIN part p ON pq.part_id = p.id
                GROUP BY pq.work_order_id
              ) part_stats ON part_stats.work_order_id = wo.id
              LEFT JOIN (
                SELECT ac.work_order_id, SUM(ac.cost) AS total_additional_cost
                FROM additional_cost ac
                GROUP BY ac.work_order_id
              ) add_stats ON add_stats.work_order_id = wo.id
              WHERE wo.company_id = :companyId
                AND wo.created_at BETWEEN :start AND :end
                AND wo.status = 3
            )
            SELECT
              a.id,
              a.name,
              SUM(wo_agg.labor_time) AS total_time,
              SUM(wo_agg.labor_cost) AS total_labor_cost,
              SUM(wo_agg.part_cost) AS total_part_cost,
              SUM(wo_agg.additional_cost) AS total_additional_cost
            FROM wo_agg
            JOIN asset a ON wo_agg.asset_id = a.id
            GROUP BY a.id, a.name
            ORDER BY  SUM(wo_agg.labor_time)
                   + SUM(wo_agg.labor_cost)
                   + SUM(wo_agg.part_cost)
                   + SUM(wo_agg.additional_cost) DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopNAssetsTimeCost(@Param("companyId") Long companyId,
                                          @Param("start") Date start,
                                          @Param("end") Date end,
                                          @Param("limit") int limit);

    @Query(value = """
            SELECT wo.id, wo.completed_on,
              COALESCE(labor_stats.total_labor_cost, 0) AS labor_cost,
              COALESCE(part_stats.total_part_cost, 0) AS part_cost,
              COALESCE(add_stats.total_additional_cost, 0) AS additional_cost
            FROM work_order wo
            LEFT JOIN (
              SELECT l.work_order_id,
                     SUM(l.hourly_rate * l.duration / 3600) AS total_labor_cost
              FROM labor l
              GROUP BY l.work_order_id
            ) labor_stats ON labor_stats.work_order_id = wo.id
            LEFT JOIN (
              SELECT pq.work_order_id, SUM(p.cost * pq.quantity) AS total_part_cost
              FROM part_quantity pq
              JOIN part p ON pq.part_id = p.id
              GROUP BY pq.work_order_id
            ) part_stats ON part_stats.work_order_id = wo.id
            LEFT JOIN (
              SELECT ac.work_order_id, SUM(ac.cost) AS total_additional_cost
              FROM additional_cost ac
              GROUP BY ac.work_order_id
            ) add_stats ON add_stats.work_order_id = wo.id
            WHERE wo.company_id = :companyId
              AND wo.completed_on BETWEEN :start AND :end
              AND wo.status = 3
            ORDER BY wo.completed_on
            """, nativeQuery = true)
    List<Object[]> findWOCostsByDateRange(@Param("companyId") Long companyId,
                                          @Param("start") Date start,
                                          @Param("end") Date end);

    @Query("SELECT CASE WHEN COUNT(wo) > :threshold THEN true ELSE false END " +
            "FROM WorkOrder wo WHERE wo.company.id = :companyId AND wo.status!=com.grash.model.enums.Status" +
            ".COMPLETE")
    boolean hasMoreActiveThan(@Param("companyId") Long companyId, @Param("threshold") Long threshold);
}
