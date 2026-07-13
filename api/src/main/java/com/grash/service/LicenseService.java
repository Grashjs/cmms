package com.grash.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.grash.utils.FingerprintGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class LicenseService {

    private final ObjectMapper objectMapper;
    @Value("${license-key:#{null}}")
    private String licenseKey;

    @Value("${license-fingerprint-required}")
    private boolean licenseFingerprintRequired;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean isLicenseValid() {
        if (licenseKey == null || licenseKey.isEmpty()) {
            return false;
        }
        try {
            String apiUrl = "https://api.keygen.sh/v1/accounts/1ca3e517-f3d8-473f-a45c-81069900acb7/licenses/actions" +
                    "/validate-key";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("application/vnd.api+json"));
            headers.setAccept(Collections.singletonList(MediaType.valueOf("application/vnd.api+json")));

            // Build the JSON body
            ObjectNode scopeNode = objectMapper.createObjectNode();
            if (licenseFingerprintRequired) {
                // Generate fingerprint
                String fingerprint = FingerprintGenerator.generateFingerprint();
                System.out.println("X-Machine-Fingerprint: " + fingerprint);
                scopeNode.put("fingerprint", fingerprint);
            }

            ObjectNode metaNode = objectMapper.createObjectNode();
            metaNode.put("key", licenseKey);
            metaNode.set("scope", scopeNode);

            ObjectNode root = objectMapper.createObjectNode();
            root.set("meta", metaNode);

            String body = objectMapper.writeValueAsString(root);

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode json = objectMapper.readTree(response.getBody());
                return json.path("meta").path("valid").asBoolean(false);
            }
        } catch (Exception e) {
            return false;
        }

        return false;
    }


    public boolean isSSOEnabled() {
        return isLicenseValid();
    }
}
