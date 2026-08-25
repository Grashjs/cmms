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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Covers the category binding of asset custom fields. Filtering the form client-side is not
 * enough: without these checks a lift-only field would end up stored on a ventilation unit.
 * The binding drops such a value rather than refusing the request — see
 * {@code docs/custom-field-categories.md} for why, and what that costs.
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
    private CustomField power;
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
        power = field(300L, "Leistung", ventilation, lift);

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

    private List<String> storedLabels() {
        return asset.getCustomFieldValues().stream()
                .map(customFieldValue -> customFieldValue.getCustomField().getLabel())
                .collect(Collectors.toList());
    }

    @Nested
    class CategoryBinding {

        @Test
        @DisplayName("accepts a field bound to the category of the asset itself")
        void fieldOfMatchingCategory_isAccepted() {
            stubFields(airflow, stops);

            setCustomFields(Collections.singletonList(value(100L, "3200")), ventilation.getId());

            assertEquals(1, asset.getCustomFieldValues().size());
            CustomFieldValue saved = asset.getCustomFieldValues().iterator().next();
            assertEquals("3200", saved.getValue());
            assertEquals(airflow, saved.getCustomField());
        }

        @Test
        @DisplayName("drops a field bound to a different category instead of failing the save")
        void fieldOfOtherCategory_isDropped() {
            stubFields(airflow, stops);

            setCustomFields(Collections.singletonList(value(200L, "12")), ventilation.getId());

            assertTrue(asset.getCustomFieldValues().isEmpty());
        }

        @Test
        @DisplayName("a submission mixing applicable and inapplicable fields keeps the applicable ones")
        void mixedSubmission_keepsApplicableFields() {
            // The upstream mobile app has no category filter and submits every asset field it
            // knows. Refusing the request made saving from that app impossible; the applicable
            // values have to survive alongside the dropped ones. Same path on update: moving an
            // asset to another category drops the values that no longer apply and keeps the
            // rest, because setCustomFields clears and rewrites the whole collection.
            stubFields(airflow, stops, power);

            setCustomFields(Arrays.asList(
                    value(100L, "3200"),
                    value(200L, "12"),
                    value(300L, "45")), ventilation.getId());

            assertEquals(Arrays.asList("Volumenstrom", "Leistung"), storedLabels());
        }

        @Test
        @DisplayName("a field bound to several categories fits each of them")
        void fieldBoundToSeveralCategories_isAcceptedForEach() {
            stubFields(power);

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
        @DisplayName("an asset without a category only keeps unbound fields")
        void assetWithoutCategory_dropsBoundFields() {
            CustomField global = field(400L, "Hersteller");
            stubFields(global, airflow);

            setCustomFields(Arrays.asList(value(400L, "Viessmann"), value(100L, "3200")), null);

            assertEquals(Collections.singletonList("Hersteller"), storedLabels());
        }

        @Test
        @DisplayName("the overload without a category filters nothing")
        void legacyOverload_doesNotFilterByCategory() {
            // Locations, meters and parts call the short signature. Their fields never carry
            // asset categories, so the distinction is invisible to them — but it matters for
            // assets: "no category supplied" is not the same as "the asset has no category". If
            // the asset path ever regressed to this overload, filtering on an unknown category
            // would silently discard everything the user typed. Falling back to pre-feature
            // behaviour — store it, let the form keep hiding it — loses no data.
            CustomField global = field(400L, "Hersteller");
            stubFields(global, airflow);

            customFieldValueService.setCustomFields(
                    asset,
                    asset.getCustomFieldValues(),
                    Arrays.asList(value(400L, "Viessmann"), value(100L, "3200")),
                    company,
                    CustomFieldEntityType.ASSET,
                    cfv -> cfv.setAsset(asset));

            assertEquals(Arrays.asList("Hersteller", "Volumenstrom"), storedLabels());
        }

        @Test
        @DisplayName("an unknown field id is still a 404")
        void unknownField_throwsNotFound() {
            stubFields(airflow);

            CustomException ex = assertThrows(CustomException.class,
                    () -> setCustomFields(Collections.singletonList(value(999L, "x")),
                            ventilation.getId()));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }
    }
}
