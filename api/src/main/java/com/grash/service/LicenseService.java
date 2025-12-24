package com.grash.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grash.dto.license.*;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.utils.FingerprintGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LicenseService {

    private final ObjectMapper objectMapper;

    @Value("${license-key:#{null}}")
    private String licenseKey;

    @Value("${license-fingerprint-required}")
    private boolean licenseFingerprintRequired;

    @Value("${keygen.account-id}")
    private String keygenAccountId;

    private final RestTemplate restTemplate = new RestTemplate();

    private LicenseValidationResponse lastLicenseResponse = null;
    private Set<String> cachedEntitlements = new HashSet<>();
    private long lastCheckedTime = 0;
    private static final long TWELVE_HOUR_MILLIS = 12 * 60 * 60 * 1000;


    public synchronized LicensingState getLicensingState() {
        long now = System.currentTimeMillis();

        if ((now - lastCheckedTime) < TWELVE_HOUR_MILLIS && lastLicenseResponse != null) {
            return LicensingState.builder().valid(lastLicenseResponse.getMeta().isValid()).entitlements(cachedEntitlements).build();
        }

        if (licenseKey == null || licenseKey.isEmpty()) {
            lastLicenseResponse = null;
            cachedEntitlements.clear();
            lastCheckedTime = now;
            return LicensingState.builder().valid(false).build();
        }

        try {
            String apiUrl = String.format(
                    "https://api.keygen.sh/v1/accounts/%s/licenses/actions/validate-key",
                    keygenAccountId
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("application/vnd.api+json"));
            headers.setAccept(Collections.singletonList(MediaType.valueOf("application/vnd.api+json")));

            LicenseValidationRequest request = buildValidationRequest();
            String body = objectMapper.writeValueAsString(request);

            HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);
            ResponseEntity<LicenseValidationResponse> response = restTemplate.postForEntity(
                    apiUrl,
                    httpEntity,
                    LicenseValidationResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                lastLicenseResponse = response.getBody();
                lastCheckedTime = now;

                // Fetch entitlements if license is valid
                if (lastLicenseResponse.getMeta().isValid()) {
                    fetchAndCacheEntitlements(lastLicenseResponse.getData().getId());
                } else {
                    cachedEntitlements.clear();
                }

                return LicensingState.builder().valid(lastLicenseResponse.getMeta().isValid()).entitlements(cachedEntitlements).build();
            }
        } catch (Exception e) {
            log.error("License validation failed", e);
            lastLicenseResponse = null;
            cachedEntitlements.clear();
        }

        lastCheckedTime = now;
        return LicensingState.builder().valid(false).build();
    }

    public boolean isSSOEnabled() {
        return hasEntitlement(LicenseEntitlement.SSO);
    }

    public boolean hasEntitlement(LicenseEntitlement entitlement) {
        if (!getLicensingState().isValid()) {
            return false;
        }

        return cachedEntitlements.contains(entitlement.toString());
    }

    private void fetchAndCacheEntitlements(String licenseId) {
        try {
            String entitlementsUrl = String.format(
                    "https://api.keygen.sh/v1/accounts/%s/licenses/%s/entitlements",
                    keygenAccountId,
                    licenseId
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.valueOf("application/vnd.api+json")));
            headers.set("Authorization", "License " + licenseKey);

            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);
            ResponseEntity<EntitlementsResponse> response = restTemplate.exchange(
                    entitlementsUrl,
                    HttpMethod.GET,
                    httpEntity,
                    EntitlementsResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                cachedEntitlements = response.getBody().getData().stream()
                        .map(EntitlementData::getAttributes)
                        .map(EntitlementAttributes::getCode)
                        .collect(Collectors.toSet());

                log.info("Cached {} entitlements: {}", cachedEntitlements.size(), cachedEntitlements);
            }
        } catch (Exception e) {
            log.error("Failed to fetch entitlements for license: {}", licenseId, e);
            cachedEntitlements.clear();
        }
    }

    private LicenseValidationRequest buildValidationRequest() {
        LicenseValidationRequest request = new LicenseValidationRequest();
        LicenseValidationMeta meta = new LicenseValidationMeta();
        meta.setKey(licenseKey);

        if (licenseFingerprintRequired) {
            String fingerprint = FingerprintGenerator.generateFingerprint();
            log.info("X-Machine-Fingerprint: {}", fingerprint);

            LicenseValidationScope scope = new LicenseValidationScope();
            scope.setFingerprint(fingerprint);
            meta.setScope(scope);
        }

        request.setMeta(meta);
        return request;
    }
}