package com.grash.service;

import com.grash.dto.license.LicenseEntitlement;
import com.grash.dto.webhookEndpoint.WebhookEndpointPatchDTO;
import com.grash.dto.webhookEndpoint.WebhookEndpointPostDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.WebhookEndpointMapper;
import com.grash.model.*;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PlanFeatures;
import com.grash.repository.WebhookEndpointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class WebhookEndpointService {

    private final WebhookEndpointRepository webhookEndpointRepository;
    private final WebhookEndpointMapper webhookEndpointMapper;
    private final LicenseService licenseService;

    public WebhookEndpoint create(WebhookEndpointPostDTO webhookEndpointReq, OwnUser user) {
        if (!(licenseService.hasEntitlement(LicenseEntitlement.WEBHOOK)
                && user.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS)
                && user.getCompany().getSubscription().getSubscriptionPlan().getFeatures()
                .contains(PlanFeatures.WEBHOOK)))
            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        WebhookEndpoint webhookEndpoint = webhookEndpointMapper.fromPostDto(webhookEndpointReq);
        webhookEndpoint.setSecret(generateWebhookSecret());
        return webhookEndpointRepository.save(webhookEndpoint);
    }

    public List<WebhookEndpoint> getActiveEndpointsByCompany(Long companyId) {
        return webhookEndpointRepository.findByCompanyIdAndEnabled(companyId, true);
    }

    public WebhookEndpoint update(Long id, WebhookEndpointPatchDTO webhookEndpointReq, OwnUser user) {
        WebhookEndpoint savedWebhookEndpoint = webhookEndpointRepository.findById(id).orElse(null);
        if (savedWebhookEndpoint != null) {
            WebhookEndpoint webhookEndpoint1 = webhookEndpointMapper.updateWebhookEndpoint(savedWebhookEndpoint,
                    webhookEndpointReq);
            return webhookEndpointRepository.save(webhookEndpoint1);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public void delete(Long id) {
        webhookEndpointRepository.deleteById(id);
    }


    public String rotateSecret(Long endpointId, Long companyId) {
        WebhookEndpoint endpoint = webhookEndpointRepository
                .findByIdAndCompanyId(endpointId, companyId)
                .orElseThrow(() -> new RuntimeException("Webhook endpoint not found"));

        String newSecret = generateWebhookSecret();
        endpoint.setSecret(newSecret);
        webhookEndpointRepository.save(endpoint);

        return newSecret;
    }

    private String generateWebhookSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return "whsec_" + HexFormat.of().formatHex(bytes);
    }

    public Optional<WebhookEndpoint> findById(Long id) {
        return webhookEndpointRepository.findById(id);
    }
}