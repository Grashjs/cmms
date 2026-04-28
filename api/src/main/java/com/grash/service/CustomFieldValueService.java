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

        List<CustomField> customFields =
                customFieldRepository.findByCompanySettingsAndEntityType(company.getCompanySettings(),
                        entityType);

        customFieldValues.clear();

        for (CustomFieldValuePostDTO customFieldValuePostDTO : customFieldValuePostDTOS) {
            CustomFieldValue newCustomFieldValue = new CustomFieldValue();
            entitySetter.accept(newCustomFieldValue);
            newCustomFieldValue.setValue(customFieldValuePostDTO.getValue());
            newCustomFieldValue.setCustomField(customFields.stream()
                    .filter(customField -> customField.getId().equals(customFieldValuePostDTO.getId()))
                    .findFirst()
                    .orElseThrow(() -> new CustomException("Custom field not found",
                            HttpStatus.NOT_FOUND)));
            customFieldValues.add(newCustomFieldValue);
        }
    }
}
