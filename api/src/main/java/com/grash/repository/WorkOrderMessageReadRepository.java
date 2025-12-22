package com.grash.repository;

import com.grash.model.OwnUser;
import com.grash.model.WorkOrderMessage;
import com.grash.model.WorkOrderMessageRead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkOrderMessageReadRepository extends JpaRepository<WorkOrderMessageRead, Long> {

    Optional<WorkOrderMessageRead> findByMessageAndUser(WorkOrderMessage message, OwnUser user);

    boolean existsByMessageAndUser(WorkOrderMessage message, OwnUser user);
}
