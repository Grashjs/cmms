package com.grash.service;

import com.grash.dto.cutomField.CustomFieldValuePostDTO;
import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.enums.CustomFieldEntityType;
import com.grash.repository.CustomFieldRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomFieldValueService {
    private final CustomFieldRepository customFieldRepository;

    /**
     * For entities whose custom fields never carry asset categories — locations, meters,
     * parts, work orders. The category is <em>unknown</em> rather than absent, so no
     * category filtering happens at all. Mirrors the {@code undefined} case of
     * {@code customFieldAppliesToCategory} in the frontend.
     */
    public void setCustomFields(
            Object entity,
            Collection<CustomFieldValue> customFieldValues,
            List<CustomFieldValuePostDTO> customFieldValuePostDTOS,
            Company company,
            CustomFieldEntityType entityType,
            Consumer<CustomFieldValue> entitySetter) {
        setCustomFields(entity, customFieldValues, customFieldValuePostDTOS, company, entityType, entitySetter,
                null, false);
    }

    /**
     * @param assetCategoryId category of the asset being saved, or null when the asset has
     *                        none. Values for fields bound to other categories are dropped
     *                        rather than stored, so a stored value always applies to its
     *                        asset's category.
     */
    public void setCustomFields(
            Object entity,
            Collection<CustomFieldValue> customFieldValues,
            List<CustomFieldValuePostDTO> customFieldValuePostDTOS,
            Company company,
            CustomFieldEntityType entityType,
            Consumer<CustomFieldValue> entitySetter,
            Long assetCategoryId) {
        setCustomFields(entity, customFieldValues, customFieldValuePostDTOS, company, entityType, entitySetter,
                assetCategoryId, true);
    }

    /**
     * A value whose field does not belong to the asset's category is <em>discarded</em>, not
     * refused. Refusing is the stricter contract and was the original behaviour, but it makes
     * the API unusable for any client that does not filter its own form: the upstream mobile
     * app submits every asset custom field it knows and would fail the whole save with a 406.
     * Dropping keeps the same guarantee about what reaches the database — the value is never
     * stored — while letting the rest of the save succeed. See docs/custom-field-categories.md.
     *
     * @param categoryKnown whether the caller could determine a category at all. False means
     *                      "not an asset, or the asset path did not supply one"; nothing is
     *                      filtered then, because dropping values on an unknown category would
     *                      turn a wiring mistake into silent data loss.
     */
    private void setCustomFields(
            Object entity,
            Collection<CustomFieldValue> customFieldValues,
            List<CustomFieldValuePostDTO> customFieldValuePostDTOS,
            Company company,
            CustomFieldEntityType entityType,
            Consumer<CustomFieldValue> entitySetter,
            Long assetCategoryId,
            boolean categoryKnown) {

        List<CustomField> customFields =
                customFieldRepository.findByCompanySettingsAndEntityTypeFetchAssetCategories(
                        company.getCompanySettings(), entityType);

        customFieldValues.clear();

        for (CustomFieldValuePostDTO customFieldValuePostDTO : customFieldValuePostDTOS) {
            CustomField customField = customFields.stream()
                    .filter(field -> field.getId().equals(customFieldValuePostDTO.getId()))
                    .findFirst()
                    .orElseThrow(() -> new CustomException("Custom field not found",
                            HttpStatus.NOT_FOUND));

            if (categoryKnown && !appliesToCategory(customField, assetCategoryId)) {
                log.warn("Dropping value for custom field \"{}\" (id {}): it is not bound to asset category {}. "
                                + "The client submitted a field its form should not have shown.",
                        customField.getLabel(), customField.getId(), assetCategoryId);
                continue;
            }

            CustomFieldValue newCustomFieldValue = new CustomFieldValue();
            entitySetter.accept(newCustomFieldValue);
            newCustomFieldValue.setValue(customFieldValuePostDTO.getValue());
            newCustomFieldValue.setCustomField(customField);
            customFieldValues.add(newCustomFieldValue);
        }
    }

    /**
     * A field with no categories applies everywhere, which is how every field behaved
     * before categories existed. A bound field only applies to the categories it lists.
     */
    private boolean appliesToCategory(CustomField customField, Long assetCategoryId) {
        if (customField.getAssetCategories().isEmpty()) {
            return true;
        }
        return assetCategoryId != null && customField.getAssetCategories().stream()
                .anyMatch(category -> category.getId().equals(assetCategoryId));
    }
}
