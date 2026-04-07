package com.grash.repository;

import com.grash.model.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.Optional;

public interface VendorRepository extends JpaRepository<Vendor, Long>, JpaSpecificationExecutor<Vendor> {
    Collection<Vendor> findByCompany_Id(Long id);

<<<<<<< HEAD
=======
    @Query("""
                SELECT MIN(v) 
                FROM Vendor v
                WHERE (LOWER(v.companyName) = LOWER(:name)
                    OR LOWER(v.name) = LOWER(:name))
                  AND v.company.id = :companyId
                ORDER BY v.createdAt
            """)
>>>>>>> 0443a7f7 (refactor: update `VendorRepository` query to return earliest matching vendor by creation date)
    Optional<Vendor> findByNameIgnoreCaseAndCompany_Id(String name, Long companyId);

    void deleteByCompany_IdAndIsDemoTrue(Long companyId);
}
