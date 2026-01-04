package com.grash.repository;

import com.grash.model.Part;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface PartRepository extends JpaRepository<Part, Long>, JpaSpecificationExecutor<Part> {
    Collection<Part> findByCompany_Id(@Param("x") Long id);

    Optional<Part> findByIdAndCompany_Id(Long id, Long companyId);

    Optional<Part> findByNameIgnoreCaseAndCompany_Id(String name, Long companyId);

    Optional<Part> findByBarcodeAndCompany_Id(String barcode, Long companyId);

    void deleteByCompany_IdAndIsDemoTrue(Long companyId);

    @Query("SELECT CASE WHEN COUNT(p) > :threshold THEN true ELSE false END " +
            "FROM Part p WHERE p.company.id = :companyId")
    boolean hasMoreThan(@Param("companyId") Long companyId, @Param("threshold") Long threshold);
}
