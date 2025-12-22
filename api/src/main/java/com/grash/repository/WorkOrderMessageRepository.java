package com.grash.repository;

import com.grash.model.WorkOrder;
import com.grash.model.WorkOrderMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkOrderMessageRepository extends JpaRepository<WorkOrderMessage, Long> {

    Page<WorkOrderMessage> findByWorkOrderOrderByCreatedAtAsc(WorkOrder workOrder, Pageable pageable);

    List<WorkOrderMessage> findByWorkOrderOrderByCreatedAtAsc(WorkOrder workOrder);

    @Query("SELECT COUNT(m) FROM WorkOrderMessage m WHERE m.workOrder = :workOrder AND m.id NOT IN " +
            "(SELECT r.message.id FROM WorkOrderMessageRead r WHERE r.user.id = :userId) AND m.user.id != :userId AND m.deleted = false")
    long countUnreadMessages(@Param("workOrder") WorkOrder workOrder, @Param("userId") Long userId);

    @Query("SELECT m FROM WorkOrderMessage m WHERE m.workOrder = :workOrder AND m.deleted = false ORDER BY m.createdAt ASC")
    List<WorkOrderMessage> findActiveMessagesByWorkOrder(@Param("workOrder") WorkOrder workOrder);
}
