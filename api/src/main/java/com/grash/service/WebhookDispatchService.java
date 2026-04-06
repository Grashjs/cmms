package com.grash.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.exception.CustomException;
import com.grash.model.Company;
import com.grash.model.WebhookEndpoint;
import com.grash.model.enums.PlanFeatures;
import com.grash.model.enums.webhook.WebhookEvent;
import com.grash.repository.WebhookEndpointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookDispatchService {

    private final WebhookEndpointRepository webhookEndpointRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    private final LicenseService licenseService;

    @Async
    public <T> void dispatchWebhook(
            Company company,
            WebhookEvent eventType,
            Map<String, Object> result,
            String serializedField,
            T rawPayload,
            Function<T, Object> mapper
    ) {
        if (!(licenseService.hasEntitlement(LicenseEntitlement.WEBHOOK) && company.getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.WEBHOOK)))
            return;
        List<WebhookEndpoint> endpoints = webhookEndpointRepository
                .findByCompanyIdAndEnabled(company.getId(), true)
                .stream()
                .filter(endpoint -> endpoint.getEvent().equals(eventType))
                .toList();
        result.put("occurredAt", new Date());
        result.put("companyId", company.getId());
        for (WebhookEndpoint endpoint : endpoints) {
            try {
                result.put(serializedField, endpoint.isSerialize() ? mapper.apply(rawPayload) : null);
                sendWebhook(endpoint, eventType, result);
                endpoint.setLastTriggeredAt(new Date());
                webhookEndpointRepository.save(endpoint);

            } catch (Exception e) {
                log.error("Failed to send webhook to: {}", endpoint.getUrl(), e);
                //TODO Consider implementing retry logic or dead letter queue
            }
        }
    }

    private void sendWebhook(WebhookEndpoint endpoint, WebhookEvent eventType, Object payload) {
        try {
            log.info("Sending webhook to: {}", endpoint.getUrl());
            String jsonPayload = objectMapper.writeValueAsString(payload);
            String signature = generateSignature(jsonPayload, endpoint.getSecret());
            String timestamp = String.valueOf(System.currentTimeMillis());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Webhook-Signature", signature);
            headers.set("X-Webhook-Timestamp", timestamp);
            headers.set("X-Webhook-Id", UUID.randomUUID().toString());
            headers.set("X-Webhook-Event", eventType.name());

            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    endpoint.getUrl(),
                    request,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Webhook failed with status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to send webhook", e);
        }
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