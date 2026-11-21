package com.grash.repository;

import com.grash.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    boolean existsByIdAndCompany_IdAndWorkOrder_Id(Long id, Long companyId, Long workOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Task t where t.id = :id and t.company.id = :companyId")
    Optional<Task> findForUpdate(@Param("id") Long id, @Param("companyId") Long companyId);

    List<Task> findByWorkOrder_IdOrderByCreatedAtAsc(Long id);

    List<Task> findByPreventiveMaintenance_Id(Long id);
}
