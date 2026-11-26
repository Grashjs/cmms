package com.grash.repository;

import com.grash.model.WebhookDeliveryLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebhookDeliveryLogRepository extends JpaRepository<WebhookDeliveryLog, Long> {
    Page<WebhookDeliveryLog> findByWebhookEndpointIdAndCompanyId(Long webhookEndpointId, Long companyId,
                                                                 Pageable pageable);

    Optional<WebhookDeliveryLog> findByIdAndCompanyId(Long id, Long companyId);
}
