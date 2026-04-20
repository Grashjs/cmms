package com.grash.repository;

import com.grash.model.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Long>, JpaSpecificationExecutor<Request> {
    Collection<Request> findByCompany_Id(@Param("x") Long id);

    Collection<Request> findByCreatedAtBetweenAndCompany_Id(Date date1, Date date2, Long id);

    @Query("SELECT COUNT(r) FROM Request r WHERE r.workOrder IS NULL AND r.cancelled = false and r.company" +
            ".id=:companyId")
    Integer countPending(@Param("companyId") Long companyId);

    List<Request> findByCategory_IdAndCreatedAtBetween(Long id, Date start, Date end);

    void deleteByCompany_IdAndIsDemoTrue(Long companyId);
}
