package com.grash.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grash.dto.license.*;
import com.grash.utils.FingerprintGenerator;
import com.grash.utils.LicenseFileValidator;
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
            "/entitlements?limit=100";
    private static final MediaType KEYGEN_MEDIA_TYPE = MediaType.valueOf("application/vnd.api+json");
    private static final String KEYGEN_PUBLIC_KEY =
            "cf9ce7f95c29c0cb0666a61d89c931bd4170a5fbaa0a391ff6649c213f4d13fc";

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${license-key:#{null}}")
    private String licenseKey;

    @Value("${license-fingerprint-required}")
    private boolean licenseFingerprintRequired;

    @Value("${keygen.account-id}")
    private String keygenAccountId;

    @Value("${license-file-path:#{null}}")
    private String licenseFilePath;

    private volatile LicenseValidationResponse cachedLicenseResponse;
    private volatile DecryptedLicenseData cachedDecryptedLicenseData;
    private volatile Set<String> cachedEntitlements = new HashSet<>();
    private volatile long lastCheckedTime = 0;

    public synchronized LicensingState getLicensingState() {
        if (isCacheValid()) {
            return buildLicensingStateFromCache();
        }

        if (!hasLicenseKey() && !hasLicenseFile()) {
            return clearCacheAndReturnInvalid();
        }

        // Try license file validation first if available
        if (hasLicenseFile()) {
            return validateAndCacheLicenseFile();
        }

        // Fall back to Keygen API validation
        return validateAndCacheLicenseKey();
    }

    public boolean isSSOEnabled() {
        return hasEntitlement(LicenseEntitlement.SSO);
    }

    public boolean hasEntitlement(LicenseEntitlement entitlement) {
    return true;
    }
    }

    private boolean isCacheValid() {
        long now = System.currentTimeMillis();
        return (now - lastCheckedTime) < CACHE_DURATION_MILLIS && (cachedLicenseResponse != null || cachedDecryptedLicenseData != null);
    }

    private boolean hasLicenseKey() {
        return licenseKey != null && !licenseKey.isEmpty();
    }

    private boolean hasLicenseFile() {
        if (licenseFilePath == null || licenseFilePath.isBlank()) return false;
        return LicenseFileValidator.licenseFileExists(licenseFilePath);
    }

    private LicensingState buildLicensingStateFromCache() {
        if (cachedDecryptedLicenseData != null) {
            return buildStateFromDecryptedLicense();
        }

        if (cachedLicenseResponse != null) {
            return buildStateFromKeygenResponse();
        }

        return clearCacheAndReturnInvalid();
    }

    private LicensingState buildStateFromDecryptedLicense() {
        if (!cachedDecryptedLicenseData.isTimeValid()) {
            log.warn("License file has expired or is invalid based on issued/expiry timestamps");
            return LicensingState.builder()
                    .hasLicense(true)
                    .valid(false)
                    .build();
        }

        Date expiry = cachedDecryptedLicenseData.getMeta().getExpiry();
        return LicensingState.builder()
                .valid(true)
                .hasLicense(true)
                .entitlements(cachedDecryptedLicenseData.getEntitlements())
                .planName(cachedDecryptedLicenseData.getLicenseName())
                .expirationDate(expiry)
                .usersCount(cachedDecryptedLicenseData.getUsersCount())
                .build();
    }

    private LicensingState buildStateFromKeygenResponse() {
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
        cachedDecryptedLicenseData = null;
        cachedEntitlements.clear();
        lastCheckedTime = System.currentTimeMillis();
        return LicensingState.builder()
                .hasLicense(false)
                .valid(false)
                .build();
    }

    private LicensingState validateAndCacheLicenseKey() {
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

    private LicensingState validateAndCacheLicenseFile() {
        long now = System.currentTimeMillis();
        try {
            // Use license key from file or configured key
            String keyToUse = licenseKey;
            if (keyToUse == null || keyToUse.isEmpty()) {
                log.warn("License key is required for license file decryption");
                return clearCacheAndReturnInvalid();
            }

            String decryptedData = LicenseFileValidator.validateAndDecryptLicenseFile(
                    licenseFilePath,
                    keyToUse,
                    KEYGEN_PUBLIC_KEY
            );

            if (decryptedData != null) {
                cachedDecryptedLicenseData = objectMapper.readValue(decryptedData, DecryptedLicenseData.class);
                lastCheckedTime = now;
                return buildStateFromDecryptedLicense();
            }
        } catch (Exception e) {
            log.error("License file validation failed", e);
            cachedDecryptedLicenseData = null;
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
