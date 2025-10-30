package com.grash.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.grash.utils.FingerprintGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class LicenseServiceTest {

    @InjectMocks
    private LicenseService licenseService;

    private ObjectMapper objectMapper = new ObjectMapper(); // Use a real ObjectMapper

    @Mock
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(licenseService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(licenseService, "objectMapper", objectMapper); // Inject real ObjectMapper
    }

    @Nested
    @DisplayName("isLicenseValid method")
    class IsLicenseValid {

        @Test
        @DisplayName("should return false when license key is null")
        void shouldReturnFalseWhenLicenseKeyIsNull() {
            ReflectionTestUtils.setField(licenseService, "licenseKey", null);
            assertFalse(licenseService.isLicenseValid());
        }

        @Test
        @DisplayName("should return false when license key is empty")
        void shouldReturnFalseWhenLicenseKeyIsEmpty() {
            ReflectionTestUtils.setField(licenseService, "licenseKey", "");
            assertFalse(licenseService.isLicenseValid());
        }

        @Test
        @DisplayName("should return cached value when checked within 12 hours")
        void shouldReturnCachedValueWhenCheckedWithin12Hours() {
            long now = System.currentTimeMillis();
            ReflectionTestUtils.setField(licenseService, "lastCheckedTime", now);
            ReflectionTestUtils.setField(licenseService, "lastLicenseValidity", true);
            assertTrue(licenseService.isLicenseValid());
        }

        @Test
        @DisplayName("should return true for valid license")
        void shouldReturnTrueForValidLicense() throws IOException {
            ReflectionTestUtils.setField(licenseService, "licenseKey", "valid-key");
            ReflectionTestUtils.setField(licenseService, "licenseFingerprintRequired", false);
            ReflectionTestUtils.setField(licenseService, "lastCheckedTime", 0L);

            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any(Class.class)))
                    .thenReturn(new ResponseEntity<>("{\"meta\":{\"valid\":true}}", HttpStatus.OK));

            boolean isValid = licenseService.isLicenseValid();

            assertTrue(isValid);
        }

        @Test
        @DisplayName("should return false for invalid license")
        void shouldReturnFalseForInvalidLicense() throws IOException {
            ReflectionTestUtils.setField(licenseService, "licenseKey", "invalid-key");
            ReflectionTestUtils.setField(licenseService, "licenseFingerprintRequired", false);
            ReflectionTestUtils.setField(licenseService, "lastCheckedTime", 0L);

            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any(Class.class)))
                    .thenReturn(new ResponseEntity<>("{\"meta\":{\"valid\":false}}", HttpStatus.OK));

            boolean isValid = licenseService.isLicenseValid();

            assertFalse(isValid);
        }

        @Test
        @DisplayName("should return false when API call fails")
        void shouldReturnFalseWhenApiCallFails() throws IOException {
            ReflectionTestUtils.setField(licenseService, "licenseKey", "any-key");
            ReflectionTestUtils.setField(licenseService, "licenseFingerprintRequired", false);
            ReflectionTestUtils.setField(licenseService, "lastCheckedTime", 0L);

            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any(Class.class)))
                    .thenThrow(new RuntimeException("API error"));

            boolean isValid = licenseService.isLicenseValid();

            assertFalse(isValid);
        }

        @Test
        @DisplayName("should include fingerprint when required")
        void shouldIncludeFingerprintWhenRequired() throws IOException {
            try (MockedStatic<FingerprintGenerator> mocked = mockStatic(FingerprintGenerator.class)) {
                mocked.when(FingerprintGenerator::generateFingerprint).thenReturn("test-fingerprint");
                ReflectionTestUtils.setField(licenseService, "licenseKey", "valid-key");
                ReflectionTestUtils.setField(licenseService, "licenseFingerprintRequired", true);
                ReflectionTestUtils.setField(licenseService, "lastCheckedTime", 0L);

                when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any(Class.class)))
                        .thenAnswer(invocation -> {
                            HttpEntity<String> requestEntity = invocation.getArgument(1);
                            String body = requestEntity.getBody();
                            JsonNode root = objectMapper.readTree(body);
                            assertEquals("test-fingerprint", root.path("meta").path("scope").path("fingerprint").asText());
                            return new ResponseEntity<>("{\"meta\":{\"valid\":true}}", HttpStatus.OK);
                        });

                assertTrue(licenseService.isLicenseValid());
            }
        }
    }

    @Nested
    @DisplayName("isSSOEnabled method")
    class IsSSOEnabled {

        @Test
        @DisplayName("should return true when license is valid")
        void shouldReturnTrueWhenLicenseIsValid() {
            LicenseService spyLicenseService = org.mockito.Mockito.spy(licenseService);
            when(spyLicenseService.isLicenseValid()).thenReturn(true);
            assertTrue(spyLicenseService.isSSOEnabled());
        }

        @Test
        @DisplayName("should return false when license is invalid")
        void shouldReturnFalseWhenLicenseIsInvalid() {
            LicenseService spyLicenseService = org.mockito.Mockito.spy(licenseService);
            when(spyLicenseService.isLicenseValid()).thenReturn(false);
            assertFalse(spyLicenseService.isSSOEnabled());
        }
    }
}