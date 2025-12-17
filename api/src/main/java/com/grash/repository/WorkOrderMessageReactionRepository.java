package com.grash.repository;

import com.grash.model.OwnUser;
import com.grash.model.WorkOrderMessage;
import com.grash.model.WorkOrderMessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkOrderMessageReactionRepository extends JpaRepository<WorkOrderMessageReaction, Long> {

    List<WorkOrderMessageReaction> findByMessage(WorkOrderMessage message);

    Optional<WorkOrderMessageReaction> findByMessageAndUserAndReaction(WorkOrderMessage message, OwnUser user, String reaction);

    void deleteByMessageAndUserAndReaction(WorkOrderMessage message, OwnUser user, String reaction);
}
