package com.grash.service;

import com.grash.dto.keygen.*;
import com.grash.dto.license.SelfHostedPlan;
import com.grash.exception.CustomException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.grash.utils.Consts.selfHostedPlans;

@Service
@RequiredArgsConstructor
public class KeygenService {

    @Value("${keygen.product-token}")
    private String keygenProductToken;

    @Value("${keygen.account-id}")
    private String keygenAccountId;

    private final RestTemplate restTemplate = new RestTemplate();

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

    @NotNull
    private static KeygenLicenseRequest getKeygenLicenseRequest(String keygenUserId, SelfHostedPlan plan,
                                                                KeygenLicenseAttributes attributes) {
        KeygenRelationshipData policyData = new KeygenRelationshipData("policies", plan.getKeygenPolicyId());
        KeygenRelationship policy = new KeygenRelationship(policyData);

        KeygenRelationshipData userData = new KeygenRelationshipData("users", keygenUserId);
        KeygenRelationship user = new KeygenRelationship(userData);

        KeygenLicenseRelationships relationships = new KeygenLicenseRelationships(policy, user);
        KeygenLicenseData data = new KeygenLicenseData("licenses", attributes, relationships);
        KeygenLicenseRequest request = new KeygenLicenseRequest(data);
        return request;
    }

    /**
     * Fetch or create a Keygen user and return their UUID
     */
    public String getOrCreateKeygenUser(String email) {
        try {
            // Try to fetch existing user first
            return fetchKeygenUserByEmail(email);
        } catch (Exception e) {
            // If user doesn't exist, create them
            KeygenUserResponse response = createKeygenUser(email);
            return response.getData().getId();
        }
    }

    /**
     * Create a new Keygen user with only email
     */
    public KeygenUserResponse createKeygenUser(String email) {
        String url = String.format("https://api.keygen.sh/v1/accounts/%s/users", keygenAccountId);

        Map<String, String> metadata = new HashMap<>();
        KeygenUserCreateAttributes attributes = new KeygenUserCreateAttributes();
        attributes.setEmail(email);
        attributes.setMetadata(metadata);

        KeygenUserCreateData data = new KeygenUserCreateData("users", attributes);
        KeygenUserCreateRequest request = new KeygenUserCreateRequest(data);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + keygenProductToken);
        headers.setContentType(MediaType.parseMediaType("application/vnd.api+json"));
        headers.set("Accept", "application/vnd.api+json");

        HttpEntity<KeygenUserCreateRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<KeygenUserResponse> response = restTemplate.postForEntity(
                url, entity, KeygenUserResponse.class);

        return response.getBody();
    }

    /**
     * Fetch a Keygen user by email
     */
    public String fetchKeygenUserByEmail(String email) {
        String url = String.format("https://api.keygen.sh/v1/accounts/%s/users/%s",
                keygenAccountId, email.trim().toLowerCase());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + keygenProductToken);
        headers.set("Accept", "application/vnd.api+json");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<KeygenUserSingleResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, KeygenUserSingleResponse.class);

        if (response.getBody() != null &&
                response.getBody().getData() != null)
            return response.getBody().getData().getId();

        throw new CustomException("User not found in Keygen", HttpStatus.NOT_FOUND);
    }

    /**
     * Updated createLicense method that validates UUID
     */
    /**
     * Create a license for a user by email
     */
    public KeygenLicenseResponse createLicense(String planId, String email, Integer quantity,
                                               String paddleSubscriptionId) {
        // Get or create the Keygen user and get their UUID
        String keygenUserId = getOrCreateKeygenUser(email);

        String url = String.format("https://api.keygen.sh/v1/accounts/%s/licenses", keygenAccountId);
        SelfHostedPlan plan = selfHostedPlans.stream()
                .filter(selfHostedPlan -> selfHostedPlan.getId().equals(planId))
                .findFirst().orElseThrow(() -> new CustomException("Plan not found", HttpStatus.BAD_REQUEST));

        Map<String, String> metadata = new HashMap<>();
        metadata.put("usersCount", quantity.toString());
        metadata.put("paddleSubscriptionId", paddleSubscriptionId);
        KeygenLicenseAttributes attributes = new KeygenLicenseAttributes(plan.getName(), metadata);
        KeygenLicenseRequest request = getKeygenLicenseRequest(keygenUserId, plan, attributes);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + keygenProductToken);
        headers.setContentType(MediaType.parseMediaType("application/vnd.api+json"));
        headers.set("Accept", "application/vnd.api+json");

        HttpEntity<KeygenLicenseRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<KeygenLicenseResponse> response = restTemplate.postForEntity(
                url, entity, KeygenLicenseResponse.class);

        return response.getBody();
    }

    public void extendLicense(String licenseId, String newExpiry) {
        String url = String.format("https://api.keygen.sh/v1/accounts/%s/licenses/%s",
                keygenAccountId, licenseId);

        KeygenLicenseUpdateAttributes attributes = new KeygenLicenseUpdateAttributes();
        attributes.setExpiry(newExpiry);

        KeygenLicenseUpdateData data = new KeygenLicenseUpdateData("licenses", attributes);
        KeygenLicenseUpdateRequest request = new KeygenLicenseUpdateRequest(data);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + keygenProductToken);
        headers.setContentType(MediaType.parseMediaType("application/vnd.api+json"));
        headers.set("Accept", "application/vnd.api+json");

        HttpEntity<KeygenLicenseUpdateRequest> entity = new HttpEntity<>(request, headers);
        restTemplate.exchange(url, HttpMethod.PATCH, entity, Map.class);
    }

    public KeygenLicenseResponseData findLicenseByUserEmailAndPolicyId(String email, String policyId) {
        try {
            String userId = fetchKeygenUserByEmail(email);
            String url = String.format("https://api.keygen.sh/v1/accounts/%s/licenses?user=%s&policy=%s",
                    keygenAccountId, userId, policyId);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + keygenProductToken);
            headers.set("Accept", "application/vnd.api+json");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<LicenseListResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, LicenseListResponse.class);

            if (response.getBody() != null &&
                    response.getBody().getData() != null && response.getBody().getData().size() == 1) {
                return response.getBody().getData().get(0);
            }
        } catch (CustomException e) {
            // User not found, so no license will be found
            return null;
        }

        return null;
    }

    public KeygenLicenseResponseData getLicenseByPaddleSubscriptionId(String paddleSubscriptionId) {
        String url = String.format("https://api.keygen.sh/v1/accounts/%s/licenses?metadata[paddleSubscriptionId]=%s",
                keygenAccountId, paddleSubscriptionId);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + keygenProductToken);
        headers.set("Accept", "application/vnd.api+json");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<LicenseListResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, LicenseListResponse.class);

        if (response.getBody() != null &&
                response.getBody().getData() != null && response.getBody().getData().size() == 1) {
            // The API returns a list of licenses, we expect only one for a unique paddleSubscriptionId
            return response.getBody().getData().get(0);
        }

        return null;
    }

    // Main response wrapper
    @Data
    public static class LicenseListResponse {
        private List<KeygenLicenseResponseData> data;
    }
}