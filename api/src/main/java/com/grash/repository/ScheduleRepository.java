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
}
