package com.grash.service;

import com.grash.dto.CategoryPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.TimeCategoryMapper;
import com.grash.model.CompanySettings;
import com.grash.model.OwnUser;
import com.grash.model.TimeCategory;
import com.grash.model.enums.RoleType;
import com.grash.repository.TimeCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TimeCategoryService {
    private final TimeCategoryRepository timeCategoryRepository;
    private final CompanySettingsService companySettingsService;
    private final TimeCategoryMapper timeCategoryMapper;

    public TimeCategory create(TimeCategory timeCategory) {
        Optional<TimeCategory> categoryWithSameName = timeCategoryRepository.findByNameIgnoreCaseAndCompanySettings_Id(timeCategory.getName(), timeCategory.getCompanySettings().getId());
        if (categoryWithSameName.isPresent()) {
            throw new CustomException("TimeCategory with same name already exists", HttpStatus.NOT_ACCEPTABLE);
        }
        return timeCategoryRepository.save(timeCategory);
    }

    public TimeCategory update(Long id, CategoryPatchDTO timeCategory) {
        if (timeCategoryRepository.existsById(id)) {
            TimeCategory savedTimeCategory = timeCategoryRepository.findById(id).get();
            return timeCategoryRepository.save(timeCategoryMapper.updateTimeCategory(savedTimeCategory, timeCategory));
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public Collection<TimeCategory> getAll() {
        return timeCategoryRepository.findAll();
    }

    public void delete(Long id) {
        timeCategoryRepository.deleteById(id);
    }

    public Optional<TimeCategory> findById(Long id) {
        return timeCategoryRepository.findById(id);
    }

    public Collection<TimeCategory> findByCompanySettings(Long id) {
        return timeCategoryRepository.findByCompanySettings_Id(id);

    }

    public boolean isTimeCategoryInCompany(TimeCategory timeCategory, long companyId, boolean optional) {
        if (optional) {
            Optional<TimeCategory> optionalTimeCategory = timeCategory == null ? Optional.empty() : findById(timeCategory.getId());
            return timeCategory == null || (optionalTimeCategory.isPresent() && optionalTimeCategory.get().getCompanySettings().getCompany().getId().equals(companyId));
        } else {
            Optional<TimeCategory> optionalTimeCategory = findById(timeCategory.getId());
            return optionalTimeCategory.isPresent() && optionalTimeCategory.get().getCompanySettings().getCompany().getId().equals(companyId);
        }
    }
}
