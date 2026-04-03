package com.grash.repository;

import com.grash.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long>,
        JpaSpecificationExecutor<ApiKey> {
    Optional<ApiKey> findByCode(String code);

    @Modifying
    @Transactional
    @Query("UPDATE ApiKey a SET a.lastUsed = :lastUsed WHERE a.id = :id")
    void updateLastUsed(@Param("id") Long id, @Param("lastUsed") Date lastUsed);
}
