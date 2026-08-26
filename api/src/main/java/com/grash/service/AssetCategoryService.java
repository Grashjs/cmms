package com.grash.service;

import com.grash.dto.CategoryPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.AssetCategoryMapper;
import com.grash.model.AssetCategory;
import com.grash.model.CompanySettings;
import com.grash.model.User;
import com.grash.repository.AssetCategoryRepository;
import com.grash.utils.Sanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AssetCategoryService {
    private final AssetCategoryRepository assetCategoryRepository;
    private final CompanySettingsService companySettingsService;
    private final AssetCategoryMapper assetCategoryMapper;

    public AssetCategory create(AssetCategory assetCategory, User user) {
        Sanitizer.sanitizeCategory(assetCategory);
        Optional<AssetCategory> categoryWithSameName =
                assetCategoryRepository.findByNameIgnoreCaseAndCompanySettings_Id(assetCategory.getName(),
                        user.getCompany().getCompanySettings().getId());
        if (categoryWithSameName.isPresent()) {
            throw new CustomException("AssetCategory with same name already exists", HttpStatus.NOT_ACCEPTABLE);
        }
        return assetCategoryRepository.save(assetCategory);
    }

    public AssetCategory update(Long id, CategoryPatchDTO assetCategory) {
        if (assetCategoryRepository.existsById(id)) {
            AssetCategory savedAssetCategory = assetCategoryRepository.findById(id).get();
            AssetCategory updatedAssetCategory = assetCategoryMapper.updateAssetCategory(savedAssetCategory,
                    assetCategory);
            Sanitizer.sanitizeCategory(updatedAssetCategory);
            return assetCategoryRepository.save(updatedAssetCategory);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public Collection<AssetCategory> getAll() {
        return assetCategoryRepository.findAll();
    }

    public void delete(Long id) {
        assetCategoryRepository.deleteById(id);
    }

    public Optional<AssetCategory> findById(Long id) {
        return assetCategoryRepository.findById(id);
    }

    public Collection<AssetCategory> findByCompanySettings(Long id) {
        return assetCategoryRepository.findByCompanySettings_Id(id);
    }

    public Optional<AssetCategory> findByNameIgnoreCaseAndCompanySettings(String category, Long companySettingsId) {
        return assetCategoryRepository.findByNameIgnoreCaseAndCompanySettings_Id(category, companySettingsId);
    }

    public AssetCategory getOrCreate(String name, CompanySettings companySettings) {
        String cleanName = Sanitizer.cleanText(name);
        return assetCategoryRepository.findByNameIgnoreCaseAndCompanySettings_Id(cleanName, companySettings.getId())
                .orElseGet(() -> assetCategoryRepository.save(new AssetCategory(cleanName, companySettings)));
    }
}
