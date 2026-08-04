package com.grash.repository;

import com.grash.model.SavedView;
import com.grash.model.enums.SavedViewEntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SavedViewRepository extends JpaRepository<SavedView, Long> {

    /**
     * Views the user may see on one list page: their own plus everything shared inside the
     * company. Filtering in the query rather than in Java keeps a company with many shared
     * views from loading all of them to discard most.
     */
    @Query("SELECT sv FROM SavedView sv WHERE sv.company.id = :companyId AND sv.entityType = :entityType "
            + "AND (sv.shared = true OR sv.owner.id = :userId) ORDER BY sv.name ASC")
    List<SavedView> findVisible(@Param("companyId") Long companyId,
                                @Param("entityType") SavedViewEntityType entityType,
                                @Param("userId") Long userId);

    boolean existsByCompany_IdAndEntityTypeAndNameIgnoreCase(Long companyId,
                                                             SavedViewEntityType entityType,
                                                             String name);
}
