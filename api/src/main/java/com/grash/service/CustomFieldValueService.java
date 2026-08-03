package com.grash.service;

import com.grash.dto.cutomField.CustomFieldValuePostDTO;
import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.enums.CustomFieldEntityType;
import com.grash.repository.CustomFieldRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class CustomFieldValueService {
    private final CustomFieldRepository customFieldRepository;

    public void setCustomFields(
            Object entity,
            Collection<CustomFieldValue> customFieldValues,
            List<CustomFieldValuePostDTO> customFieldValuePostDTOS,
            Company company,
            CustomFieldEntityType entityType,
            Consumer<CustomFieldValue> entitySetter) {
        setCustomFields(entity, customFieldValues, customFieldValuePostDTOS, company, entityType, entitySetter,
                null);
    }

    /**
     * @param assetCategoryId category of the asset being saved, or null when the entity has
     *                        no category or is not an asset. Fields bound to other
     *                        categories are rejected — without this the category binding
     *                        would only filter the form and the API would still accept any
     *                        field for any asset.
     */
    public void setCustomFields(
            Object entity,
            Collection<CustomFieldValue> customFieldValues,
            List<CustomFieldValuePostDTO> customFieldValuePostDTOS,
            Company company,
            CustomFieldEntityType entityType,
            Consumer<CustomFieldValue> entitySetter,
            Long assetCategoryId) {

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

            if (!appliesToCategory(customField, assetCategoryId)) {
                throw new CustomException("The custom field \"" + customField.getLabel()
                        + "\" does not apply to this asset's category", HttpStatus.NOT_ACCEPTABLE);
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
