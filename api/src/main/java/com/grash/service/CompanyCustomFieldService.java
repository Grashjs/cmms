package com.grash.service;

import com.grash.dto.CompanyCustomFieldPatchDTO;
import com.grash.dto.CompanyCustomFieldPostDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.CompanyCustomFieldMapper;
import com.grash.model.CompanyCustomField;
import com.grash.model.CompanySettings;
import com.grash.model.enums.CustomFieldType;
import com.grash.repository.CompanyCustomFieldRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CompanyCustomFieldService {
    private final CompanyCustomFieldRepository companyCustomFieldRepository;
    private final CompanyCustomFieldMapper companyCustomFieldMapper;

    public CompanyCustomField create(CompanyCustomField companyCustomField) {
        return companyCustomFieldRepository.save(companyCustomField);
    }

    public CompanyCustomField create(CompanyCustomFieldPostDTO dto, CompanySettings companySettings) {
        CompanyCustomField field = companyCustomFieldMapper.toModel(dto);
        field.setCompanySettings(companySettings);
        return companyCustomFieldRepository.save(field);
    }

    public CompanyCustomField update(Long id, CompanyCustomFieldPatchDTO companyCustomFieldPatchDTO) {
        if (companyCustomFieldRepository.existsById(id)) {
            CompanyCustomField savedField = companyCustomFieldRepository.findById(id).get();
            return companyCustomFieldRepository.save(companyCustomFieldMapper.updateCompanyCustomField(savedField, companyCustomFieldPatchDTO));
        } else throw new CustomException("Custom field not found", HttpStatus.NOT_FOUND);
    }

    public Page<CompanyCustomField> getAllByCompanySettings(CompanySettings companySettings, Pageable pageable) {
        return companyCustomFieldRepository.findByCompanySettings(companySettings, pageable);
    }

    public List<CompanyCustomField> getAllByCompanySettings(CompanySettings companySettings) {
        return companyCustomFieldRepository.findByCompanySettings(companySettings);
    }

    public Page<CompanyCustomField> getByFieldType(CompanySettings companySettings, CustomFieldType fieldType, Pageable pageable) {
        return companyCustomFieldRepository.findByCompanySettingsAndFieldType(companySettings, fieldType, pageable);
    }

    public List<CompanyCustomField> getByFieldType(CompanySettings companySettings, CustomFieldType fieldType) {
        return companyCustomFieldRepository.findByCompanySettingsAndFieldType(companySettings, fieldType);
    }

    public void delete(Long id) {
        companyCustomFieldRepository.deleteById(id);
    }

    public Optional<CompanyCustomField> findById(Long id) {
        return companyCustomFieldRepository.findById(id);
    }
}

