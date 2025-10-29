package com.grash.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grash.dto.BrandConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandingServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private LicenseService licenseService;

    @InjectMocks
    private BrandingService brandingService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(brandingService, "customColors", null);
        ReflectionTestUtils.setField(brandingService, "brandRawConfig", null);
    }

    @Test
    void getMailBackgroundColor_whenNoCustomColors() {
        assertEquals("#00A0E3", brandingService.getMailBackgroundColor());
    }

    @Test
    void getMailBackgroundColor_whenCustomColorsExist() {
        ReflectionTestUtils.setField(brandingService, "customColors", "{\"emailColors\":\"#FFFFFF\"}");
        assertEquals("#FFFFFF", brandingService.getMailBackgroundColor());
    }

    @Test
    void getBrandConfig_whenLicenseIsInvalid() {
        when(licenseService.isLicenseValid()).thenReturn(false);

        BrandConfig brandConfig = brandingService.getBrandConfig();

        assertEquals("Atlas CMMS", brandConfig.getName());
        assertEquals("Atlas", brandConfig.getShortName());
    }

    @Test
    void getBrandConfig_whenLicenseIsValidAndNoBrandRawConfig() {
        when(licenseService.isLicenseValid()).thenReturn(true);

        BrandConfig brandConfig = brandingService.getBrandConfig();

        assertEquals("Atlas CMMS", brandConfig.getName());
        assertEquals("Atlas", brandConfig.getShortName());
    }

    @Test
    void getBrandConfig_whenLicenseIsValidAndBrandRawConfigExists() throws Exception {
        when(licenseService.isLicenseValid()).thenReturn(true);
        ReflectionTestUtils.setField(brandingService, "brandRawConfig", "{\"name\":\"Test Brand\",\"shortName\":\"TB\"}");

        BrandConfig expectedBrandConfig = BrandConfig.builder().name("Test Brand").shortName("TB").build();
        when(objectMapper.readValue("{\"name\":\"Test Brand\",\"shortName\":\"TB\"}", BrandConfig.class))
                .thenReturn(expectedBrandConfig);

        BrandConfig brandConfig = brandingService.getBrandConfig();

        assertEquals("Test Brand", brandConfig.getName());
        assertEquals("TB", brandConfig.getShortName());
    }
}
