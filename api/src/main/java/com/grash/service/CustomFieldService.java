package com.grash.service;

import com.grash.dto.cutomField.CustomFieldPatchDTO;
import com.grash.dto.cutomField.CustomFieldPostDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.CustomFieldMapper;
import com.grash.model.AssetCategory;
import com.grash.model.CustomField;
import com.grash.model.CompanySettings;
import com.grash.model.enums.CustomFieldEntityType;
import com.grash.model.enums.CustomFieldType;
import com.grash.repository.AssetCategoryRepository;
import com.grash.repository.CustomFieldRepository;
import com.grash.utils.Sanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomFieldService {
    private final CustomFieldRepository customFieldRepository;
    private final CustomFieldMapper customFieldMapper;
    private final AssetCategoryRepository assetCategoryRepository;

    public CustomField create(CustomField customField) {
        Sanitizer.sanitizeCustomField(customField);
        return customFieldRepository.save(customField);
    }

    public CustomField create(CustomFieldPostDTO dto, CompanySettings companySettings) {
        CustomField field = customFieldMapper.toModel(dto);
        Sanitizer.sanitizeCustomField(field);
        field.setCompanySettings(companySettings);
        field.setOrder(customFieldRepository.countByCompanySettings_IdAndEntityType(companySettings.getId(),
                field.getEntityType()));
        field.setAssetCategories(resolveAssetCategories(dto.getAssetCategoryIds(), field));
        return customFieldRepository.save(field);
    }

    public CustomField update(Long id, CustomFieldPatchDTO customFieldPatchDTO) {
        if (customFieldRepository.existsById(id)) {
            CustomField savedField = customFieldRepository.findById(id).get();
            CustomField patched = customFieldMapper.updateCustomField(savedField, customFieldPatchDTO);
            // Null means "leave the binding alone"; an empty list deliberately clears it and
            // makes the field apply to every asset again.
            if (customFieldPatchDTO.getAssetCategoryIds() != null) {
                patched.setAssetCategories(
                        resolveAssetCategories(customFieldPatchDTO.getAssetCategoryIds(), patched));
            }
            Sanitizer.sanitizeCustomField(patched);
            return customFieldRepository.save(patched);
        } else throw new CustomException("Custom field not found", HttpStatus.NOT_FOUND);
    }

    /**
     * Turns the ids sent by the client into managed categories, rejecting anything that
     * does not belong to the field's own company. Without that check a caller could bind a
     * field to a foreign company's category and learn its id by trial and error.
     */
    private List<AssetCategory> resolveAssetCategories(List<Long> categoryIds, CustomField field) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return new ArrayList<>();
        }
        if (field.getEntityType() != CustomFieldEntityType.ASSET) {
            throw new CustomException("Asset categories can only be assigned to fields of entity type ASSET",
                    HttpStatus.NOT_ACCEPTABLE);
        }
        Long companySettingsId = field.getCompanySettings().getId();
        List<AssetCategory> resolved = new ArrayList<>();
        for (Long categoryId : new LinkedHashSet<>(categoryIds)) {
            AssetCategory category = assetCategoryRepository.findById(categoryId)
                    .orElseThrow(() -> new CustomException("Asset category " + categoryId + " not found",
                            HttpStatus.NOT_FOUND));
            if (!category.getCompanySettings().getId().equals(companySettingsId)) {
                throw new CustomException("Asset category " + categoryId + " belongs to another company",
                        HttpStatus.FORBIDDEN);
            }
            resolved.add(category);
        }
        return resolved;
    }

    public Page<CustomField> getAllByCompanySettings(CompanySettings companySettings, Pageable pageable) {
        return customFieldRepository.findByCompanySettings(companySettings, pageable);
    }

    public List<CustomField> getAllByCompanySettings(CompanySettings companySettings) {
        return customFieldRepository.findByCompanySettingsFetchAssetCategories(companySettings);
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

