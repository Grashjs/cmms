package com.grash.repository;

import com.grash.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CommentRepository extends JpaRepository<Comment, Long>,
        JpaSpecificationExecutor<Comment> {
    
    long countByWorkOrderId(Long workOrderId);
}
