package com.grash.repository;

import com.grash.model.WebhookEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WebhookEndpointRepository extends JpaRepository<WebhookEndpoint, Long> {
    List<WebhookEndpoint> findByCompanyIdAndEnabled(Long companyId, boolean enabled);

    Optional<WebhookEndpoint> findByIdAndCompanyId(Long id, Long companyId);
}