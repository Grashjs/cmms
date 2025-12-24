package com.grash.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grash.controller.*;
import com.grash.dto.keygen.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KeygenService {

    @Value("${keygen.product-token}")
    private String keygenProductToken;

    @Value("${keygen.account-id}")
    private String keygenAccountId;

    @Value("${keygen.policy-id}")
    private String keygenPolicyId;

    private final RestTemplate restTemplate = new RestTemplate();

    public KeygenEventResponse fetchWebhookEvent(String eventId) {
        String url = String.format("https://api.keygen.sh/v1/accounts/%s/webhook-events/%s",
                keygenAccountId, eventId);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + keygenProductToken);
        headers.set("Accept", "application/vnd.api+json");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<KeygenEventResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, KeygenEventResponse.class);

        return response.getBody();
    }

    public void updateUserMetadata(String userId, Map<String, String> metadata) {
        String url = String.format("https://api.keygen.sh/v1/accounts/%s/users/%s",
                keygenAccountId, userId);

        KeygenUserUpdateAttributes attributes = new KeygenUserUpdateAttributes(metadata);
        KeygenUserUpdateData data = new KeygenUserUpdateData("users", attributes);
        KeygenUserUpdateRequest request = new KeygenUserUpdateRequest(data);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + keygenProductToken);
        headers.setContentType(MediaType.parseMediaType("application/vnd.api+json"));
        headers.set("Accept", "application/vnd.api+json");

        HttpEntity<KeygenUserUpdateRequest> entity = new HttpEntity<>(request, headers);
        restTemplate.exchange(url, HttpMethod.PATCH, entity, Map.class);
    }

    public KeygenLicenseResponse createLicense(String userId, String subscriptionId) {
        String url = String.format("https://api.keygen.sh/v1/accounts/%s/licenses", keygenAccountId);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("stripeSubscriptionId", subscriptionId);
        KeygenLicenseAttributes attributes = new KeygenLicenseAttributes(metadata);

        KeygenRelationshipData policyData = new KeygenRelationshipData("policies", keygenPolicyId);
        KeygenRelationship policy = new KeygenRelationship(policyData);

        KeygenRelationshipData userData = new KeygenRelationshipData("users", userId);
        KeygenRelationship user = new KeygenRelationship(userData);

        KeygenLicenseRelationships relationships = new KeygenLicenseRelationships(policy, user);
        KeygenLicenseData data = new KeygenLicenseData("licenses", attributes, relationships);
        KeygenLicenseRequest request = new KeygenLicenseRequest(data);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + keygenProductToken);
        headers.setContentType(MediaType.parseMediaType("application/vnd.api+json"));
        headers.set("Accept", "application/vnd.api+json");

        HttpEntity<KeygenLicenseRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<KeygenLicenseResponse> response = restTemplate.postForEntity(
                url, entity, KeygenLicenseResponse.class);

        return response.getBody();
    }
}