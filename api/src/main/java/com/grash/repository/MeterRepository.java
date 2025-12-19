package com.grash.repository;

import com.grash.model.Meter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.Optional;

public interface MeterRepository extends JpaRepository<Meter, Long>, JpaSpecificationExecutor<Meter> {
    Collection<Meter> findByCompany_Id(Long id);

    Collection<Meter> findByAsset_Id(Long id);

    Optional<Meter> findByIdAndCompany_Id(Long id, Long companyId);

    void deleteByCompany_IdAndIsDemoTrue(Long companyId);
}
