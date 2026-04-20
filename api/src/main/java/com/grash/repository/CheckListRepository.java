package com.grash.repository;

import com.grash.model.Checklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface CheckListRepository extends JpaRepository<Checklist, Long> {
    Collection<Checklist> findByCompanySettings_Id(Long id);

    @Query("SELECT CASE WHEN COUNT(c) > :threshold THEN true ELSE false END " +
            "FROM Checklist c WHERE c.companySettings.company.id = :companyId")
    boolean hasMoreThan(@Param("companyId") Long companyId, @Param("threshold") Long threshold);
}
