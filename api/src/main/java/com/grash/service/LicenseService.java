package com.grash.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grash.dto.license.*;
import com.grash.utils.FingerprintGenerator;
import com.grash.utils.Helper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LicenseService {

    private static final long CACHE_DURATION_MILLIS = 12 * 60 * 60 * 1000; // 12 hours
    private static final String API_URL_TEMPLATE = "https://api.keygen.sh/v1/accounts/%s/licenses/actions/validate-key";
    private static final String ENTITLEMENTS_URL_TEMPLATE = "https://api.keygen.sh/v1/accounts/%s/licenses/%s" +
            "/entitlements";
    private static final MediaType KEYGEN_MEDIA_TYPE = MediaType.valueOf("application/vnd.api+json");

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${license-key:#{null}}")
    private String licenseKey;

    @Value("${license-fingerprint-required}")
    private boolean licenseFingerprintRequired;

    @Value("${keygen.account-id}")
    private String keygenAccountId;

    private volatile LicenseValidationResponse cachedLicenseResponse;
    private volatile Set<String> cachedEntitlements = new HashSet<>();
    private volatile long lastCheckedTime = 0;

    public synchronized LicensingState getLicensingState() {
        //TODO remove
        if (true) return LicensingState.builder()
                .valid(true)
                .hasLicense(true)
                .expirationDate(Helper.incrementDays(new Date(), 365))
                .planName("Free")
                .entitlements(Arrays.stream(LicenseEntitlement.values()).map(Enum::toString).collect(Collectors.toSet()))
                .usersCount(5000000).build();
        if (isCacheValid()) {
            return buildLicensingStateFromCache();
        }

        if (!hasLicenseKey()) {
            return clearCacheAndReturnInvalid();
        }

        return validateAndCacheLicense();
    }

    public boolean isSSOEnabled() {
        return hasEntitlement(LicenseEntitlement.SSO);
    }

    public boolean hasEntitlement(LicenseEntitlement entitlement) {
        LicensingState state = getLicensingState();
        return state.isValid() && state.getEntitlements().contains(entitlement.toString());
    }

    private boolean isCacheValid() {
        long now = System.currentTimeMillis();
        return (now - lastCheckedTime) < CACHE_DURATION_MILLIS && cachedLicenseResponse != null;
    }

    private boolean hasLicenseKey() {
        return licenseKey != null && !licenseKey.isEmpty();
    }

    private LicensingState buildLicensingStateFromCache() {
        String rawExpiry = cachedLicenseResponse.getData().getAttributes().getExpiry();
        return LicensingState.builder()
                .valid(cachedLicenseResponse.getMeta().isValid())
                .hasLicense(true)
                .entitlements(cachedEntitlements)
                .planName(cachedLicenseResponse.getData().getAttributes().getName())
                .expirationDate(rawExpiry == null ? null : Date.from(Instant.parse(rawExpiry)))
                .usersCount(extractUsersCount(cachedLicenseResponse))
                .build();
    }

    private LicensingState clearCacheAndReturnInvalid() {
        cachedLicenseResponse = null;
        cachedEntitlements.clear();
        lastCheckedTime = System.currentTimeMillis();
        return LicensingState.builder()
                .hasLicense(false)
                .valid(false)
                .build();
    }

    private LicensingState validateAndCacheLicense() {
        long now = System.currentTimeMillis();

        try {
            Optional<LicenseValidationResponse> response = performLicenseValidation();

            if (response.isPresent()) {
                cachedLicenseResponse = response.get();
                lastCheckedTime = now;

                if (cachedLicenseResponse.getMeta().isValid()) {
                    fetchAndCacheEntitlements(cachedLicenseResponse.getData().getId());
                } else {
                    cachedEntitlements.clear();
                }
                String rawExpiry = cachedLicenseResponse.getData().getAttributes().getExpiry();

                return LicensingState.builder()
                        .hasLicense(true)
                        .valid(cachedLicenseResponse.getMeta().isValid())
                        .entitlements(cachedEntitlements)
                        .planName(cachedLicenseResponse.getData().getAttributes().getName())
                        .expirationDate(rawExpiry == null ? null : Date.from(Instant.parse(rawExpiry)))
                        .usersCount(extractUsersCount(cachedLicenseResponse))
                        .build();
            }
        } catch (Exception e) {
            log.error("License validation failed", e);
            cachedLicenseResponse = null;
            cachedEntitlements.clear();
        }

        lastCheckedTime = now;
        return LicensingState.builder()
                .hasLicense(true)
                .valid(false)
                .build();
    }

    private Optional<LicenseValidationResponse> performLicenseValidation() throws Exception {
        String apiUrl = String.format(API_URL_TEMPLATE, keygenAccountId);
        HttpEntity<String> httpEntity = createValidationRequestEntity();

        ResponseEntity<LicenseValidationResponse> response = restTemplate.postForEntity(
                apiUrl,
                httpEntity,
                LicenseValidationResponse.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            return Optional.ofNullable(response.getBody());
        }

        return Optional.empty();
    }

    private HttpEntity<String> createValidationRequestEntity() throws Exception {
        HttpHeaders headers = createKeygenHeaders();
        LicenseValidationRequest request = buildValidationRequest();
        String body = objectMapper.writeValueAsString(request);
        return new HttpEntity<>(body, headers);
    }

    private HttpHeaders createKeygenHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(KEYGEN_MEDIA_TYPE);
        headers.setAccept(Collections.singletonList(KEYGEN_MEDIA_TYPE));
        return headers;
    }

    private LicenseValidationRequest buildValidationRequest() {
        LicenseValidationRequest request = new LicenseValidationRequest();
        LicenseValidationMeta meta = new LicenseValidationMeta();
        meta.setKey(licenseKey);

        if (licenseFingerprintRequired) {
            addFingerprintToMeta(meta);
        }

        request.setMeta(meta);
        return request;
    }

    private void addFingerprintToMeta(LicenseValidationMeta meta) {
        String fingerprint = FingerprintGenerator.generateFingerprint();
        log.info("X-Machine-Fingerprint: {}", fingerprint);

        LicenseValidationScope scope = new LicenseValidationScope();
        scope.setFingerprint(fingerprint);
        meta.setScope(scope);
    }

    private void fetchAndCacheEntitlements(String licenseId) {
        try {
            String entitlementsUrl = String.format(ENTITLEMENTS_URL_TEMPLATE, keygenAccountId, licenseId);
            HttpEntity<?> httpEntity = createEntitlementsRequestEntity();

            ResponseEntity<EntitlementsResponse> response = restTemplate.exchange(
                    entitlementsUrl,
                    HttpMethod.GET,
                    httpEntity,
                    EntitlementsResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                cacheEntitlements(response.getBody());
            }
        } catch (Exception e) {
            log.error("Failed to fetch entitlements for license: {}", licenseId, e);
            cachedEntitlements.clear();
        }
    }

    private HttpEntity<?> createEntitlementsRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(KEYGEN_MEDIA_TYPE));
        headers.set("Authorization", "License " + licenseKey);
        return new HttpEntity<>(headers);
    }

    private void cacheEntitlements(EntitlementsResponse response) {
        cachedEntitlements = response.getData().stream()
                .map(EntitlementData::getAttributes)
                .map(EntitlementAttributes::getCode)
                .collect(Collectors.toSet());

        log.info("Cached {} entitlements: {}", cachedEntitlements.size(), cachedEntitlements);
    }

    private int extractUsersCount(LicenseValidationResponse response) {
        try {
            Object usersCountObj = response.getData().getAttributes().getMetadata().get("usersCount");
            return Integer.parseInt(String.valueOf(usersCountObj));
        } catch (Exception e) {
            log.warn("Failed to extract usersCount from license metadata", e);
            return 0;
        }
    }
}