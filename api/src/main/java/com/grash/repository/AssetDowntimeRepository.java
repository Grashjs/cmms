package com.grash.repository;

import com.grash.model.AssetDowntime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Date;

public interface AssetDowntimeRepository extends JpaRepository<AssetDowntime, Long> {

    List<AssetDowntime> findByAsset_Id(Long id);

    @Query("SELECT ad FROM AssetDowntime ad WHERE ad.company.id = :id AND ad.duration != 0")
    List<AssetDowntime> findByCompany_Id(@Param("id") Long id);

    @Query("SELECT ad FROM AssetDowntime ad WHERE ad.startsOn BETWEEN :date1 AND :date2 AND ad.company.id = :id AND " +
            "ad.duration != 0")
    List<AssetDowntime> findByStartsOnBetweenAndCompany_Id(@Param("date1") Date date1, @Param("date2") Date date2,
                                                           @Param("id") Long id);

    @Query("SELECT ad FROM AssetDowntime ad WHERE ad.asset.id = :id AND ad.startsOn BETWEEN :start AND :end AND ad" +
            ".duration != 0")
    List<AssetDowntime> findByAsset_IdAndStartsOnBetween(@Param("id") Long id, @Param("start") Date start, @Param(
            "end") Date end);

    @Query(value = """
            SELECT a.id, a.name,
              COUNT(ad.id) AS cnt,
              COALESCE(SUM(GREATEST(0, EXTRACT(EPOCH FROM (
                LEAST(ad.starts_on + (ad.duration * INTERVAL '1 second'), :end) -
                GREATEST(ad.starts_on, :start)
              )))), 0) AS total_duration,
              EXTRACT(EPOCH FROM (:end - GREATEST(COALESCE(a.in_service_date, a.created_at), :start))) AS living_time
            FROM asset a
            LEFT JOIN asset_downtime ad ON ad.asset_id = a.id
              AND ad.starts_on <= :end
              AND (ad.starts_on + (ad.duration * INTERVAL '1 second')) >= :start
            WHERE a.company_id = :companyId
              AND a.created_at < :end
            GROUP BY a.id, a.name, a.in_service_date, a.created_at
            HAVING COUNT(ad.id) > 0
            ORDER BY cnt DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopNAssetsByDowntime(@Param("companyId") Long companyId,
                                            @Param("start") Date start,
                                            @Param("end") Date end,
                                            @Param("limit") int limit);

    @Query(value = """
            SELECT a.id, a.name, COUNT(ad.id) AS cnt
            FROM asset a
            LEFT JOIN asset_downtime ad ON ad.asset_id = a.id AND ad.starts_on BETWEEN :start AND :end
            WHERE a.company_id = :companyId AND a.created_at < :end
            GROUP BY a.id, a.name
            HAVING COUNT(ad.id) >= 2
            ORDER BY cnt DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopNAssetsForMTBF(@Param("companyId") Long companyId,
                                         @Param("start") Date start,
                                         @Param("end") Date end,
                                         @Param("limit") int limit);

    @Query(value = """
            WITH wo_costs AS (
              SELECT wo_data.asset_id,
                COALESCE(SUM(wo_data.labor_cost), 0) AS labor_cost,
                COALESCE(SUM(wo_data.part_cost), 0) AS part_cost,
                COALESCE(SUM(wo_data.additional_cost), 0) AS additional_cost
              FROM (
                SELECT wo.asset_id,
                  COALESCE(labor_stats.total_labor_cost, 0) AS labor_cost,
                  COALESCE(part_stats.total_part_cost, 0) AS part_cost,
                  COALESCE(add_stats.total_additional_cost, 0) AS additional_cost
                FROM work_order wo
                LEFT JOIN (
                  SELECT l.work_order_id, SUM(l.hourly_rate * l.duration / 3600) AS total_labor_cost
                  FROM labor l GROUP BY l.work_order_id
                ) labor_stats ON labor_stats.work_order_id = wo.id
                LEFT JOIN (
                  SELECT pq.work_order_id, SUM(p.cost * pq.quantity) AS total_part_cost
                  FROM part_quantity pq JOIN part p ON pq.part_id = p.id
                  GROUP BY pq.work_order_id
                ) part_stats ON part_stats.work_order_id = wo.id
                LEFT JOIN (
                  SELECT ac.work_order_id, SUM(ac.cost) AS total_additional_cost
                  FROM additional_cost ac GROUP BY ac.work_order_id
                ) add_stats ON add_stats.work_order_id = wo.id
                WHERE wo.company_id = :companyId
                  AND wo.created_at BETWEEN :start AND :end
                  AND wo.status = 3
              ) wo_data
              GROUP BY wo_data.asset_id
            ),
            downtime_agg AS (
              SELECT ad.asset_id,
                COALESCE(SUM(GREATEST(0, EXTRACT(EPOCH FROM (
                  LEAST(ad.starts_on + (ad.duration * INTERVAL '1 second'), :end) -
                  GREATEST(ad.starts_on, :start)
                )))), 0) AS total_duration
              FROM asset_downtime ad
              WHERE ad.starts_on <= :end
                AND (ad.starts_on + (ad.duration * INTERVAL '1 second')) >= :start
              GROUP BY ad.asset_id
            )
            SELECT a.id, a.name,
              COALESCE(downtime_agg.total_duration, 0) AS duration,
              COALESCE(wo_costs.labor_cost, 0) AS labor_cost,
              COALESCE(wo_costs.part_cost, 0) AS part_cost,
              COALESCE(wo_costs.additional_cost, 0) AS additional_cost
            FROM asset a
            LEFT JOIN wo_costs ON wo_costs.asset_id = a.id
            LEFT JOIN downtime_agg ON downtime_agg.asset_id = a.id
            WHERE a.company_id = :companyId AND a.created_at < :end
              AND (wo_costs.asset_id IS NOT NULL OR downtime_agg.asset_id IS NOT NULL)
            ORDER BY duration DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopNAssetsDowntimeAndCosts(@Param("companyId") Long companyId,
                                                  @Param("start") Date start,
                                                  @Param("end") Date end,
                                                  @Param("limit") int limit);

}
