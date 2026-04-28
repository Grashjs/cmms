package com.grash.repository;

import com.grash.model.Company;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    List<Company> findByDemoTrue();

    void deleteAllByDemoTrue();

    Optional<Company> findBySubscription_Id(Long id);

    @EntityGraph(attributePaths = {
            "subscription",
            "subscription.subscriptionPlan",
            "subscription.subscriptionPlan.features"
    })
    @Query("SELECT c FROM Company c WHERE c.id = :id")
    Optional<Company> findByIdWithSubscription(@Param("id") Long id);

    @Query(value = """
            select exists(select 1 from company c join work_order wo on c.id = wo.company_id where wo.is_demo=false
            AND wo.parent_preventive_maintenance_id is null and c.demo=false group by c.id having count(wo.id) >= 5)
            """, nativeQuery = true)
    boolean existsAtLeastOneWithMinWorkOrders();

    @Query("SELECT u.company from User u WHERE lower(u.email) = lower(:email) AND u.ownsCompany = true")
    Optional<Company> findByOwnerEmailAndOwnsCompany(@Param("email") String email);
}
