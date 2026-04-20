package com.grash.repository;

import com.grash.model.PreventiveMaintenance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface PreventiveMaintenanceRepository extends JpaRepository<PreventiveMaintenance, Long>,
        JpaSpecificationExecutor<PreventiveMaintenance> {
    Collection<PreventiveMaintenance> findByCompany_Id(@Param("x") Long id);

    List<PreventiveMaintenance> findByCreatedAtBeforeAndCompany_Id(Date start, Long companyId);

    void deleteByCompany_IdAndIsDemoTrue(Long companyId);

    Optional<PreventiveMaintenance> findByIdAndCompany_Id(Long id, Long companyId);

    List<PreventiveMaintenance> findByIdInAndCompany_Id(List<Long> ids, Long companyId);

    @Query("SELECT CASE WHEN COUNT(p) > :threshold THEN true ELSE false END " +
            "FROM PreventiveMaintenance p WHERE p.company.id = :companyId")
    boolean hasMoreThan(@Param("companyId") Long companyId, @Param("threshold") Long threshold);

    @Query("SELECT p FROM PreventiveMaintenance p LEFT JOIN FETCH p.schedule " +
            "LEFT JOIN FETCH p.team LEFT JOIN FETCH p.primaryUser LEFT JOIN FETCH p.category " +
            "LEFT JOIN FETCH p.location LEFT JOIN FETCH p.asset " +
            "WHERE p.company.id = :companyId")
    List<PreventiveMaintenance> findByCompanyForExport(@Param("companyId") Long companyId);
}
