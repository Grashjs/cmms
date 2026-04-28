package com.grash.service;

import com.grash.dto.cutomField.CustomFieldPatchDTO;
import com.grash.dto.cutomField.CustomFieldPostDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.CustomFieldMapper;
import com.grash.model.CustomField;
import com.grash.model.CompanySettings;
import com.grash.model.enums.CustomFieldType;
import com.grash.repository.CustomFieldRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomFieldService {
    private final CustomFieldRepository customFieldRepository;
    private final CustomFieldMapper customFieldMapper;

    public CustomField create(CustomField customField) {
        return customFieldRepository.save(customField);
    }

    public CustomField create(CustomFieldPostDTO dto, CompanySettings companySettings) {
        CustomField field = customFieldMapper.toModel(dto);
        field.setCompanySettings(companySettings);
        field.setOrder(customFieldRepository.countByCompanySettings_IdAndEntityType(companySettings.getId(),
                field.getEntityType()));
        return customFieldRepository.save(field);
    }

    public CustomField update(Long id, CustomFieldPatchDTO customFieldPatchDTO) {
        if (customFieldRepository.existsById(id)) {
            CustomField savedField = customFieldRepository.findById(id).get();
            return customFieldRepository.save(customFieldMapper.updateCustomField(savedField, customFieldPatchDTO));
        } else throw new CustomException("Custom field not found", HttpStatus.NOT_FOUND);
    }

    public Page<CustomField> getAllByCompanySettings(CompanySettings companySettings, Pageable pageable) {
        return customFieldRepository.findByCompanySettings(companySettings, pageable);
    }

    public List<CustomField> getAllByCompanySettings(CompanySettings companySettings) {
        return customFieldRepository.findByCompanySettings(companySettings);
    }


    public void delete(Long id) {
        customFieldRepository.deleteById(id);
    }

    public Optional<CustomField> findById(Long id) {
        return customFieldRepository.findById(id);
    }

    @Transactional
    public void reorder(List<Long> orderedIds, CompanySettings companySettings) {
        List<CustomField> fieldsToSave = new ArrayList<>();

        for (int i = 0; i < orderedIds.size(); i++) {
            Long id = orderedIds.get(i);
            CustomField field = customFieldRepository.findById(id)
                    .orElseThrow(() -> new CustomException("Custom field not found", HttpStatus.NOT_FOUND));
            if (!field.getCompanySettings().getId().equals(companySettings.getId())) {
                throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            }
            field.setOrder(i);
            fieldsToSave.add(field);
        }

        customFieldRepository.saveAll(fieldsToSave);
    }
}

