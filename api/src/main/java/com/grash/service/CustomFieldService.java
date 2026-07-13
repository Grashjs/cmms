package com.grash.service;

import com.grash.dto.CustomFieldPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.CustomFieldMapper;
import com.grash.model.CustomField;
import com.grash.model.OwnUser;
import com.grash.model.enums.RoleType;
import com.grash.repository.CustomFieldRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomFieldService {
    private final CustomFieldRepository customFieldRepository;
    private final VendorService vendorService;
    private final CustomFieldMapper customFieldMapper;

    public CustomField create(CustomField CustomField) {
        return customFieldRepository.save(CustomField);
    }

    public CustomField update(Long id, CustomFieldPatchDTO customField) {
        if (customFieldRepository.existsById(id)) {
            CustomField savedCustomField = customFieldRepository.findById(id).get();
            return customFieldRepository.save(customFieldMapper.updateCustomField(savedCustomField, customField));
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public Collection<CustomField> getAll() {
        return customFieldRepository.findAll();
    }

    public void delete(Long id) {
        customFieldRepository.deleteById(id);
    }

    public Optional<CustomField> findById(Long id) {
        return customFieldRepository.findById(id);
    }
}
