package com.grash.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.exception.CustomException;
import com.grash.model.Company;
import com.grash.model.WebhookDeliveryLog;
import com.grash.model.WebhookEndpoint;
import com.grash.model.WorkOrderCategory;
import com.grash.model.enums.AssetStatus;
import com.grash.model.enums.PlanFeatures;
import com.grash.model.enums.Status;
import com.grash.model.enums.webhook.PartField;
import com.grash.model.enums.webhook.WOField;
import com.grash.model.enums.webhook.WebhookEvent;
import com.grash.repository.CompanyRepository;
import com.grash.repository.WebhookDeliveryLogRepository;
import com.grash.repository.WebhookEndpointRepository;
import com.grash.utils.WebhookUrlValidator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookDispatchService {

    static final int MAX_RESPONSE_BODY_LENGTH = 4000;
    private static final int MAX_ERROR_LENGTH = 1000;

    private final WebhookEndpointRepository webhookEndpointRepository;
    private final CompanyRepository companyRepository;
    private final WebhookDeliveryLogRepository webhookDeliveryLogRepository;
    @Setter(AccessLevel.PACKAGE)
    private RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    private final LicenseService licenseService;

    @Async
    public void dispatchWebhook(
            Company company,
            WebhookEvent eventType,
            Map<String, Object> result,
            String serializedField,
            Object serializedPayload,
            Collection<WOField> changedFields,
            AssetStatus assetStatus,
            Status workOrderStatus,
            Collection<WorkOrderCategory> workOrderCategories,
            Collection<PartField> partFields
    ) {
        // Reload company in the async thread's session to avoid LazyInitializationException
        Company managedCompany = companyRepository.findByIdWithSubscription(company.getId()).orElse(null);
        if (managedCompany == null) return;

        if (!(licenseService.hasEntitlement(LicenseEntitlement.WEBHOOK) && managedCompany.getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.WEBHOOK)))
            return;
        List<WebhookEndpoint> endpoints = webhookEndpointRepository
                .findByCompanyIdAndEnabled(managedCompany.getId(), true)
                .stream()
                .filter(endpoint -> endpoint.getEvent().equals(eventType))
                .filter(endpoint -> {
                    if (changedFields != null && !changedFields.isEmpty()
                            && endpoint.getWoFields() != null && !endpoint.getWoFields().isEmpty()) {
                        if (changedFields.stream().noneMatch(endpoint.getWoFields()::contains)) {
                            return false;
                        }
                    }
                    if (assetStatus != null
                            && endpoint.getAssetStatuses() != null && !endpoint.getAssetStatuses().isEmpty()) {
                        if (!endpoint.getAssetStatuses().contains(assetStatus)) {
                            return false;
                        }
                    }
                    if (workOrderStatus != null
                            && endpoint.getWorkOrderStatuses() != null && !endpoint.getWorkOrderStatuses().isEmpty()) {
                        if (!endpoint.getWorkOrderStatuses().contains(workOrderStatus)) {
                            return false;
                        }
                    }
                    if (workOrderCategories != null && !workOrderCategories.isEmpty()
                            && endpoint.getWorkOrderCategories() != null && !endpoint.getWorkOrderCategories().isEmpty()) {
                        Set<Long> endpointCategoryIds = endpoint.getWorkOrderCategories().stream()
                                .map(WorkOrderCategory::getId)
                                .collect(Collectors.toSet());
                        if (workOrderCategories.stream().noneMatch(cat -> endpointCategoryIds.contains(cat.getId()))) {
                            return false;
                        }
                    }
                    if (partFields != null && !partFields.isEmpty()
                            && endpoint.getPartFields() != null && !endpoint.getPartFields().isEmpty()) {
                        if (partFields.stream().noneMatch(endpoint.getPartFields()::contains)) {
                            return false;
                        }
                    }
                    return true;
                })
                .toList();
        result.put("occurredAt", new Date());
        result.put("companyId", managedCompany.getId());
        for (WebhookEndpoint endpoint : endpoints) {
            try {
                result.put(serializedField, endpoint.isSerialize() ? serializedPayload : null);
                String jsonPayload = objectMapper.writeValueAsString(result);
                deliver(endpoint, eventType.name(), jsonPayload);
            } catch (Exception e) {
                log.error("Failed to send webhook to: {}", endpoint.getUrl(), e);
            }
        }
    }

    /**
     * Sends the payload (already serialized) to the endpoint synchronously and records the attempt.
     * Never throws because of delivery errors: failures are stored in the returned log.
     * Reused by the manual retry. License checks are the responsibility of the callers.
     */
    public WebhookDeliveryLog deliver(WebhookEndpoint endpoint, String eventType, String jsonPayload) {
        WebhookDeliveryLog deliveryLog = new WebhookDeliveryLog();
        deliveryLog.setWebhookEndpoint(endpoint);
        deliveryLog.setCompany(endpoint.getCompany());
        deliveryLog.setEventType(eventType);
        deliveryLog.setPayload(jsonPayload);
        long start = System.nanoTime();
        try {
            WebhookUrlValidator.validate(endpoint.getUrl());
            log.info("Sending webhook to: {}", endpoint.getUrl());
            String signature = generateSignature(jsonPayload, endpoint.getSecret());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Webhook-Signature", signature);
            headers.set("X-Atlas-Signature", signature);
            headers.set("X-Webhook-Timestamp", String.valueOf(System.currentTimeMillis()));
            headers.set("X-Webhook-Id", UUID.randomUUID().toString());
            headers.set("X-Webhook-Event", eventType);

            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);
            try {
                ResponseEntity<String> response = restTemplate.postForEntity(endpoint.getUrl(), request,
                        String.class);
                deliveryLog.setResponseStatus(response.getStatusCode().value());
                deliveryLog.setResponseBody(truncate(response.getBody(), MAX_RESPONSE_BODY_LENGTH));
                deliveryLog.setSuccess(response.getStatusCode().is2xxSuccessful());
                if (!deliveryLog.isSuccess()) {
                    deliveryLog.setErrorMessage("Webhook failed with status: " + response.getStatusCode());
                }
            } catch (RestClientResponseException e) {
                deliveryLog.setResponseStatus(e.getStatusCode().value());
                deliveryLog.setResponseBody(truncate(e.getResponseBodyAsString(), MAX_RESPONSE_BODY_LENGTH));
                deliveryLog.setSuccess(false);
                deliveryLog.setErrorMessage(truncate(e.getMessage(), MAX_ERROR_LENGTH));
            }
        } catch (Exception e) {
            deliveryLog.setSuccess(false);
            deliveryLog.setErrorMessage(truncate(e.getMessage() != null ? e.getMessage() : e.toString(),
                    MAX_ERROR_LENGTH));
        }
        deliveryLog.setDurationMs((System.nanoTime() - start) / 1_000_000);
        WebhookDeliveryLog saved = webhookDeliveryLogRepository.save(deliveryLog);
        if (saved.isSuccess()) {
            endpoint.setLastTriggeredAt(new Date());
            webhookEndpointRepository.save(endpoint);
        }
        return saved;
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) return value;
        return value.substring(0, max);
    }

    private String generateSignature(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate signature", e);
        }
    }
}
