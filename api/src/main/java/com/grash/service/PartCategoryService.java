package com.grash.service;

import com.grash.dto.CategoryPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.PartCategoryMapper;
import com.grash.model.PartCategory;
import com.grash.repository.PartCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PartCategoryService {
    private final PartCategoryRepository partCategoryRepository;
    private final CompanySettingsService companySettingsService;
    private final PartCategoryMapper partCategoryMapper;

    public PartCategory create(PartCategory partCategory) {
        Optional<PartCategory> categoryWithSameName = partCategoryRepository.findByNameIgnoreCaseAndCompanySettings_Id(partCategory.getName(), partCategory.getCompanySettings().getId());
        if (categoryWithSameName.isPresent()) {
            throw new CustomException("PartCategory with same name already exists", HttpStatus.NOT_ACCEPTABLE);
        }
        return partCategoryRepository.save(partCategory);
    }

    public PartCategory update(Long id, CategoryPatchDTO partCategory) {
        if (partCategoryRepository.existsById(id)) {
            PartCategory savedPartCategory = partCategoryRepository.findById(id).get();
            return partCategoryRepository.save(partCategoryMapper.updatePartCategory(savedPartCategory, partCategory));
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public Collection<PartCategory> getAll() {
        return partCategoryRepository.findAll();
    }

    public void delete(Long id) {
        partCategoryRepository.deleteById(id);
    }

    public Optional<PartCategory> findById(Long id) {
        return partCategoryRepository.findById(id);
    }

    public Collection<PartCategory> findByCompanySettings(Long id) {
        return partCategoryRepository.findByCompanySettings_Id(id);
    }

    public Optional<PartCategory> findByNameIgnoreCaseAndCompanySettings(String category, Long companySettingsId) {
        return partCategoryRepository.findByNameIgnoreCaseAndCompanySettings_Id(category, companySettingsId);
    }
}
