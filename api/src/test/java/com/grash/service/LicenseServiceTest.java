package com.grash.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.dto.license.LicensingState;
import com.grash.repository.KeygenRequestTrackerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

// AGPLv3 fork contract tests: LicenseService returns a local, deterministic
// state with every entitlement, no external validation, no license key/file.
@ExtendWith(MockitoExtension.class)
class LicenseServiceTest {

    @InjectMocks
    private LicenseService licenseService;

    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private KeygenRequestTrackerRepository keygenRequestTrackerRepository;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        // No LICENSE_KEY, no license file path -> purely local policy
        ReflectionTestUtils.setField(licenseService, "licenseKey", null);
        ReflectionTestUtils.setField(licenseService, "licenseFingerprintRequired", false);
        ReflectionTestUtils.setField(licenseService, "keygenAccountId", "not-used");
        ReflectionTestUtils.setField(licenseService, "licenseFilePath", null);
    }

    @Test
    void state_isValidAndHasNoCommercialLicense() {
        LicensingState state = licenseService.getLicensingState();

        assertTrue(state.isValid());
        assertFalse(state.isHasLicense());
        assertEquals("AGPLv3", state.getPlanName());
        assertNull(state.getExpirationDate());
    }

    @Test
    void usersCount_isMaxValue_soNoEffectiveSeatCap() {
        LicensingState state = licenseService.getLicensingState();

        assertEquals(Integer.MAX_VALUE, state.getUsersCount());
    }

    @Test
    void everyEntitlement_isEnabled() {
        LicensingState state = licenseService.getLicensingState();
        Set<String> expected = Arrays.stream(LicenseEntitlement.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertEquals(expected, state.getEntitlements());
        for (LicenseEntitlement e : LicenseEntitlement.values()) {
            assertTrue(licenseService.hasEntitlement(e),
                    "Entitlement should be granted: " + e);
        }
    }

    @Test
    void isSSOEnabled_isTrue() {
        assertTrue(licenseService.isSSOEnabled());
    }

    @Test
    void newEnumValueIsAutoGranted() {
        // Simulates an upstream-added entitlement: the policy uses values(), so any
        // new enum constant is automatically included without code changes.
        Set<String> fromValues = Arrays.stream(LicenseEntitlement.values())
                .map(Enum::name)
                .collect(Collectors.toSet());
        LicensingState state = licenseService.getLicensingState();

        assertTrue(state.getEntitlements().containsAll(fromValues));
        assertEquals(fromValues.size(), EnumSet.allOf(LicenseEntitlement.class).size());
    }

    @Test
    void repeatedCalls_areStableAndPure() {
        LicensingState first = licenseService.getLicensingState();
        LicensingState second = licenseService.getLicensingState();

        assertEquals(first.getEntitlements(), second.getEntitlements());
        assertTrue(first.isValid());
        assertTrue(second.isValid());
        assertEquals(first.getUsersCount(), second.getUsersCount());
        assertEquals(first.getPlanName(), second.getPlanName());
    }

    @Test
    void noKeygenInteraction_forPolicyDecision() {
        // Even with credentials configured (as in application-test.yml), the local
        // policy short-circuits, so the Keygen tracker repository is never touched.
        licenseService.getLicensingState();

        org.mockito.Mockito.verifyNoInteractions(keygenRequestTrackerRepository);
    }
}