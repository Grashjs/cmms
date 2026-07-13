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

}
