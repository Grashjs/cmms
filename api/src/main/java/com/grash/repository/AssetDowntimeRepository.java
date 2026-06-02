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

    @Query("SELECT ad FROM AssetDowntime ad WHERE ad.startsOn BETWEEN :date1 AND :date2 AND ad.company.id = :id AND ad.duration != 0")
    List<AssetDowntime> findByStartsOnBetweenAndCompany_Id(@Param("date1") Date date1, @Param("date2") Date date2, @Param("id") Long id);

    @Query("SELECT ad FROM AssetDowntime ad WHERE ad.asset.id = :id AND ad.startsOn BETWEEN :start AND :end AND ad.duration != 0")
    List<AssetDowntime> findByAsset_IdAndStartsOnBetween(@Param("id") Long id, @Param("start") Date start, @Param("end") Date end);

    @Query(value = """
            SELECT a.id, a.name,
              COUNT(ad.id) AS cnt,
              COALESCE(SUM(GREATEST(0, EXTRACT(EPOCH FROM (
                LEAST(ad.starts_on + (ad.duration * INTERVAL '1 second'), :end::timestamp) -
                GREATEST(ad.starts_on, :start::timestamp)
              )))), 0) AS total_duration,
              EXTRACT(EPOCH FROM (:end::timestamp - GREATEST(COALESCE(a.in_service_date, a.created_at), :start::timestamp))) AS living_time
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

}
