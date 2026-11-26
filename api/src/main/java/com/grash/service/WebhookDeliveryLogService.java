package com.grash.service;

import com.grash.dto.license.LicenseEntitlement;
import com.grash.exception.CustomException;
import com.grash.model.User;
import com.grash.model.WebhookDeliveryLog;
import com.grash.model.WebhookEndpoint;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PlanFeatures;
import com.grash.repository.WebhookDeliveryLogRepository;
import com.grash.repository.WebhookEndpointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebhookDeliveryLogService {

    private final WebhookDeliveryLogRepository webhookDeliveryLogRepository;
    private final WebhookEndpointRepository webhookEndpointRepository;
    private final WebhookDispatchService webhookDispatchService;
    private final LicenseService licenseService;

    public Page<WebhookDeliveryLog> findByEndpoint(Long endpointId, Pageable pageable, User user) {
        checkAccess(user);
        Long companyId = user.getCompany().getId();
        webhookEndpointRepository.findByIdAndCompanyId(endpointId, companyId)
                .orElseThrow(() -> new CustomException("Webhook endpoint not found", HttpStatus.NOT_FOUND));
        return webhookDeliveryLogRepository.findByWebhookEndpointIdAndCompanyId(endpointId, companyId, pageable);
    }

    /**
     * Re-sends the stored payload synchronously and returns the NEW log.
     */
    public WebhookDeliveryLog retry(Long logId, User user) {
        checkAccess(user);
        WebhookDeliveryLog original = webhookDeliveryLogRepository
                .findByIdAndCompanyId(logId, user.getCompany().getId())
                .orElseThrow(() -> new CustomException("Webhook delivery log not found", HttpStatus.NOT_FOUND));
        WebhookEndpoint endpoint = original.getWebhookEndpoint();
        return webhookDispatchService.deliver(endpoint, original.getEventType(), original.getPayload());
    }

    // Same license/plan/permission gate as WebhookEndpointService
    private void checkAccess(User user) {
        if (!(licenseService.hasEntitlement(LicenseEntitlement.WEBHOOK)
                && user.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS)
                && user.getCompany().getSubscription().getSubscriptionPlan().getFeatures()
                .contains(PlanFeatures.WEBHOOK)))
            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }
}
