package com.grash.repository;

import com.grash.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    @Query("SELECT s from Schedule s where s.preventiveMaintenance.company.id = :x ")
    Collection<Schedule> findByCompany_Id(@Param("x") Long id);

    void deleteByPreventiveMaintenanceCompany_IdAndIsDemoTrue(Long companyId);

    @Query(value = "SELECT s.* FROM schedule s " +
            "LEFT JOIN (" +
            "    SELECT parent_preventive_maintenance_id, " +
            "           COUNT(*) as total, " +
            "           MAX(first_time_to_react) as last_react " +
            "    FROM (" +
            "        SELECT parent_preventive_maintenance_id, first_time_to_react, " +
            "               ROW_NUMBER() OVER (PARTITION BY parent_preventive_maintenance_id ORDER BY created_at " +
            "DESC) as rn " +
            "        FROM work_order " +
            "    ) ranked_wo " +
            "    WHERE rn <= 10 " +
            "    GROUP BY parent_preventive_maintenance_id " +
            ") wo_stats ON s.preventive_maintenance_id = wo_stats.parent_preventive_maintenance_id " +
            "WHERE s.disabled = false " +
            "AND (s.ends_on IS NULL OR s.ends_on > CURRENT_DATE) " +
            "AND (wo_stats.total IS NULL OR wo_stats.total < 10 OR wo_stats.last_react IS NOT NULL)", nativeQuery =
            true)
    Collection<Schedule> findByActive();

}
