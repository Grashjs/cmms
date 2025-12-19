package com.grash.repository;

import com.grash.model.WorkOrder;
import com.grash.model.enums.Priority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Date;
import java.util.Optional;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long>, JpaSpecificationExecutor<WorkOrder> {
    Collection<WorkOrder> findByCompany_Id(Long id);

    Collection<WorkOrder> findByAsset_Id(Long id);

    Collection<WorkOrder> findByLocation_Id(Long id);

    Page<WorkOrder> findByParentPreventiveMaintenance_Id(Long id, Pageable pageable);

    Collection<WorkOrder> findByPrimaryUser_Id(Long id);

    Collection<WorkOrder> findByCompletedBy_Id(Long id);

    Collection<WorkOrder> findByPriorityAndCompany_Id(Priority priority, Long companyId);

    Collection<WorkOrder> findByCategory_Id(Long id);

    Collection<WorkOrder> findByCompletedOnBetweenAndCompany_Id(Date date1, Date date2, Long id);

    Collection<WorkOrder> findByCreatedBy(Long id);

    Collection<WorkOrder> findByDueDateBetweenAndCompany_Id(Date date1, Date date2, Long id);

    Optional<WorkOrder> findByIdAndCompany_Id(Long id, Long companyId);

    Collection<WorkOrder> findByCreatedByAndCreatedAtBetween(Long id, Date date1, Date date2);

    Collection<WorkOrder> findByCompletedBy_IdAndCreatedAtBetween(Long id, Date date1, Date date2);

    @Query("SELECT DISTINCT wo FROM WorkOrder wo " +
            "LEFT JOIN wo.assignedTo assigned " +
            "LEFT JOIN wo.team team " +
            "WHERE wo.primaryUser.id = :id " +
            "OR assigned.id = :id " +
            "OR :id IN (SELECT user.id FROM team.users user)")
    Collection<WorkOrder> findByAssignedToUser(@Param("id") Long id);

    @Query("SELECT DISTINCT wo FROM WorkOrder wo " +
            "LEFT JOIN wo.assignedTo assigned " +
            "LEFT JOIN wo.team team " +
            "WHERE (wo.primaryUser.id = :id " +
            "OR assigned.id = :id " +
            "OR :id IN (SELECT user.id FROM team.users user)) AND wo.createdAt between :start and :end")
    Collection<WorkOrder> findByAssignedToUserAndCreatedAtBetween(@Param("id") Long id, @Param("start") Date start,
                                                                  @Param("end") Date end);

    Collection<WorkOrder> findByAsset_IdAndCreatedAtBetween(Long id, Date start, Date end);

    Collection<WorkOrder> findByCompany_IdAndCreatedAtBetween(Long id, Date start, Date end);

    Collection<WorkOrder> findByPriorityAndCompany_IdAndCreatedAtBetween(Priority priority, Long companyId,
                                                                         Date start, Date end);

    Collection<WorkOrder> findByCategory_IdAndCreatedAtBetween(Long id, Date start, Date end);

    void deleteByCompany_IdAndIsDemoTrue(Long companyId);
}
