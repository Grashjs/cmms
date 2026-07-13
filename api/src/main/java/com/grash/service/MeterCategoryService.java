package com.grash.service;

import com.grash.dto.CategoryPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.MeterCategoryMapper;
import com.grash.model.MeterCategory;
import com.grash.model.OwnUser;
import com.grash.model.enums.RoleType;
import com.grash.repository.MeterCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MeterCategoryService {
    private final MeterCategoryRepository meterCategoryRepository;

    private final CompanySettingsService companySettingsService;
    private final MeterCategoryMapper meterCategoryMapper;

    public MeterCategory create(MeterCategory meterCategory) {
        Optional<MeterCategory> categoryWithSameName = meterCategoryRepository.findByNameIgnoreCaseAndCompanySettings_Id(meterCategory.getName(), meterCategory.getCompanySettings().getId());
        if (categoryWithSameName.isPresent()) {
            throw new CustomException("MeterCategory with same name already exists", HttpStatus.NOT_ACCEPTABLE);
        }
        return meterCategoryRepository.save(meterCategory);
    }

    public MeterCategory update(Long id, CategoryPatchDTO meterCategory) {
        if (meterCategoryRepository.existsById(id)) {
            MeterCategory savedMeterCategory = meterCategoryRepository.findById(id).get();
            return meterCategoryRepository.save(meterCategoryMapper.updateMeterCategory(savedMeterCategory, meterCategory));
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public Collection<MeterCategory> getAll() {
        return meterCategoryRepository.findAll();
    }

    public void delete(Long id) {
        meterCategoryRepository.deleteById(id);
    }

    public Optional<MeterCategory> findById(Long id) {
        return meterCategoryRepository.findById(id);
    }

    public Collection<MeterCategory> findByCompany(Long id) {
        return meterCategoryRepository.findByCompany_Id(id);
    }

    public Optional<MeterCategory> findByNameIgnoreCaseAndCompanySettings(String name, Long companySettingsId) {
        return meterCategoryRepository.findByNameIgnoreCaseAndCompanySettings_Id(name, companySettingsId);
    }
}
