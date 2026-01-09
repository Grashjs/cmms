package com.grash.repository;

import com.grash.model.Meter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface MeterRepository extends JpaRepository<Meter, Long>, JpaSpecificationExecutor<Meter> {
    Collection<Meter> findByCompany_Id(Long id);

    Collection<Meter> findByAsset_Id(Long id);

    Optional<Meter> findByIdAndCompany_Id(Long id, Long companyId);

    void deleteByCompany_IdAndIsDemoTrue(Long companyId);

    @Query("SELECT CASE WHEN COUNT(m) > :threshold THEN true ELSE false END " +
            "FROM Meter m WHERE m.company.id = :companyId")
    boolean hasMoreThan(@Param("companyId") Long companyId, @Param("threshold") Long threshold);
}
