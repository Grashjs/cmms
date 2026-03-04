package com.grash.repository;

import com.grash.model.RequestPortal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RequestPortalRepository extends JpaRepository<RequestPortal, Long>,
        JpaSpecificationExecutor<RequestPortal> {
}
