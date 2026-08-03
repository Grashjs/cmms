package com.grash.service;

import com.grash.dto.cutomField.CustomFieldValuePostDTO;
import com.grash.exception.CustomException;
import com.grash.model.Asset;
import com.grash.model.AssetCategory;
import com.grash.model.Company;
import com.grash.model.CompanySettings;
import com.grash.model.CustomField;
import com.grash.model.CustomFieldValue;
import com.grash.model.enums.CustomFieldEntityType;
import com.grash.model.enums.CustomFieldType;
import com.grash.repository.CustomFieldRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Covers the category binding of asset custom fields. Filtering the form client-side is
 * not enough: without these checks the API would still accept a lift-only field on a
 * ventilation unit.
 */
@ExtendWith(MockitoExtension.class)
class CustomFieldValueServiceTest {

    @Mock
    private CustomFieldRepository customFieldRepository;

    @InjectMocks
    private CustomFieldValueService customFieldValueService;

    private Company company;
    private AssetCategory ventilation;
    private AssetCategory lift;
    private CustomField airflow;
    private CustomField stops;
    private CustomField manufacturer;
    private Asset asset;

    private static AssetCategory category(Long id, String name) {
        AssetCategory category = new AssetCategory();
        category.setId(id);
        category.setName(name);
        return category;
    }

    private static CustomField field(Long id, String label, AssetCategory... categories) {
        CustomField field = new CustomField();
        field.setId(id);
        field.setLabel(label);
        field.setFieldType(CustomFieldType.NUMBER);
        field.setEntityType(CustomFieldEntityType.ASSET);
        field.setAssetCategories(new ArrayList<>(Arrays.asList(categories)));
        return field;
    }

    @BeforeEach
    void setUp() {
        CompanySettings companySettings = new CompanySettings();
        companySettings.setId(1L);
        company = new Company();
        company.setId(1L);
        company.setCompanySettings(companySettings);

        ventilation = category(10L, "Raumlufttechnik");
        lift = category(20L, "Aufzug");

        airflow = field(100L, "Volumenstrom", ventilation);
        stops = field(200L, "Anzahl Haltestellen", lift);
        // Bound to both — the case that motivated many-to-many instead of a single category.
        manufacturer = field(300L, "Leistung", ventilation, lift);

        asset = new Asset();
        asset.setId(5L);
    }

    private void stubFields(CustomField... fields) {
        when(customFieldRepository.findByCompanySettingsAndEntityTypeFetchAssetCategories(
                any(CompanySettings.class), eq(CustomFieldEntityType.ASSET)))
                .thenReturn(new ArrayList<>(Arrays.asList(fields)));
    }

    private void setCustomFields(List<CustomFieldValuePostDTO> dtos, Long categoryId) {
        customFieldValueService.setCustomFields(
                asset,
                asset.getCustomFieldValues(),
                dtos,
                company,
                CustomFieldEntityType.ASSET,
                cfv -> cfv.setAsset(asset),
                categoryId);
    }

    private static CustomFieldValuePostDTO value(Long fieldId, String value) {
        CustomFieldValuePostDTO dto = new CustomFieldValuePostDTO();
        dto.setId(fieldId);
        dto.setValue(value);
        return dto;
    }

    @Nested
    class CategoryBinding {

        @Test
        @DisplayName("accepts a field bound to the asset's own category")
        void fieldOfMatchingCategory_isAccepted() {
            stubFields(airflow, stops);

            setCustomFields(Collections.singletonList(value(100L, "3200")), ventilation.getId());

            assertEquals(1, asset.getCustomFieldValues().size());
            CustomFieldValue saved = asset.getCustomFieldValues().iterator().next();
            assertEquals("3200", saved.getValue());
            assertEquals(airflow, saved.getCustomField());
        }

        @Test
        @DisplayName("rejects a field bound to a different category")
        void fieldOfOtherCategory_throws() {
            stubFields(airflow, stops);

            CustomException ex = assertThrows(CustomException.class,
                    () -> setCustomFields(Collections.singletonList(value(200L, "12")),
                            ventilation.getId()));

            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
            assertTrue(ex.getMessage().contains("Anzahl Haltestellen"));
            assertTrue(asset.getCustomFieldValues().isEmpty());
        }

        @Test
        @DisplayName("a field bound to several categories fits each of them")
        void fieldBoundToSeveralCategories_isAcceptedForEach() {
            stubFields(manufacturer);

            setCustomFields(Collections.singletonList(value(300L, "45")), ventilation.getId());
            assertEquals(1, asset.getCustomFieldValues().size());

            setCustomFields(Collections.singletonList(value(300L, "45")), lift.getId());
            assertEquals(1, asset.getCustomFieldValues().size());
        }

        @Test
        @DisplayName("an unbound field still applies to every asset")
        void fieldWithoutCategories_appliesEverywhere() {
            CustomField global = field(400L, "Hersteller");
            stubFields(global);

            setCustomFields(Collections.singletonList(value(400L, "Viessmann")), lift.getId());

            assertEquals(1, asset.getCustomFieldValues().size());
        }

        @Test
        @DisplayName("an asset without a category only takes unbound fields")
        void assetWithoutCategory_rejectsBoundFields() {
            CustomField global = field(400L, "Hersteller");
            stubFields(global, airflow);

            setCustomFields(Collections.singletonList(value(400L, "Viessmann")), null);
            assertEquals(1, asset.getCustomFieldValues().size());

            CustomException ex = assertThrows(CustomException.class,
                    () -> setCustomFields(Collections.singletonList(value(100L, "3200")), null));
            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        @DisplayName("the overload without a category behaves like an asset without one")
        void legacyOverload_treatsEntityAsUncategorised() {
            // Locations, meters and parts still call the short signature. Their fields never
            // carry asset categories, so nothing changes for them — but if the asset path
            // ever regressed to this overload, bound fields would be refused rather than
            // silently accepted. Pinning that here makes such a regression loud.
            CustomField global = field(400L, "Hersteller");
            stubFields(global, airflow);

            customFieldValueService.setCustomFields(
                    asset,
                    asset.getCustomFieldValues(),
                    Collections.singletonList(value(400L, "Viessmann")),
                    company,
                    CustomFieldEntityType.ASSET,
                    cfv -> cfv.setAsset(asset));
            assertEquals(1, asset.getCustomFieldValues().size());

            CustomException ex = assertThrows(CustomException.class,
                    () -> customFieldValueService.setCustomFields(
                            asset,
                            asset.getCustomFieldValues(),
                            Collections.singletonList(value(100L, "3200")),
                            company,
                            CustomFieldEntityType.ASSET,
                            cfv -> cfv.setAsset(asset)));
            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        @DisplayName("an unknown field id is still a 404, not a category error")
        void unknownField_throwsNotFound() {
            stubFields(airflow);

            CustomException ex = assertThrows(CustomException.class,
                    () -> setCustomFields(Collections.singletonList(value(999L, "x")),
                            ventilation.getId()));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }
    }
}
