package com.grash.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntercomService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${intercom.token:}")
    private String intercomToken;

    private String intercomApiUrl;

    @PostConstruct
    public void init() {
        this.intercomApiUrl = "https://api.intercom.io";
    }

    /**
     * Create a company activation event
     *
     * @param eventName the name of the activation event
     * @param companyId the company ID
     * @param email     the user email
     * @param metadata  additional metadata for the event
     */
    @Async
    public void createCompanyActivationEvent(String eventName, Long companyId, String email,
                                             Map<String, Object> metadata) {
        if (intercomToken == null || intercomToken.isBlank()) {
            log.warn("Intercom token not configured, skipping activation event creation");
            return;
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("event_name", eventName);
            requestBody.put("created_at", Instant.now().getEpochSecond());
            requestBody.put("user_id", email);
            requestBody.put("email", email);

            if (metadata == null) {
                metadata = new HashMap<>();
            }
            metadata.put("company_id", companyId);
            requestBody.put("metadata", metadata);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(intercomToken);
            headers.set("Intercom-Version", "2.14");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    intercomApiUrl + "/events",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {
                log.info("Intercom company activation event created successfully: {}", eventName);
            } else {
                log.error("Failed to create Intercom activation event: Status={}, Body={}",
                        response.getStatusCode(), response.getBody());
            }

        } catch (Exception e) {
            log.error("Error creating Intercom activation event: {}", e.getMessage(), e);
        }
    }
}
