package com.grash.repository;

import com.grash.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long>,
        JpaSpecificationExecutor<ApiKey> {
    Optional<ApiKey> findByCode(String code);
}
