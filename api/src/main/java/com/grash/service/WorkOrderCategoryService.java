package com.grash.service;

import com.grash.dto.CategoryPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.WorkOrderCategoryMapper;
import com.grash.model.OwnUser;
import com.grash.model.WorkOrderCategory;
import com.grash.model.enums.RoleType;
import com.grash.repository.WorkOrderCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WorkOrderCategoryService {
    private final WorkOrderCategoryRepository workOrderCategoryRepository;

    private final CompanySettingsService companySettingsService;
    private final WorkOrderCategoryMapper workOrderCategoryMapper;

    public WorkOrderCategory create(WorkOrderCategory workOrderCategory) {
        Optional<WorkOrderCategory> categoryWithSameName = workOrderCategoryRepository.findByNameIgnoreCaseAndCompanySettings_Id(workOrderCategory.getName(), workOrderCategory.getCompanySettings().getId());
        if (categoryWithSameName.isPresent()) {
            throw new CustomException("WorkOrderCategory with same name already exists", HttpStatus.NOT_ACCEPTABLE);
        }
        return workOrderCategoryRepository.save(workOrderCategory);
    }

    public WorkOrderCategory update(Long id, CategoryPatchDTO workOrderCategory) {
        if (workOrderCategoryRepository.existsById(id)) {
            WorkOrderCategory saveWorkOrderCategory = workOrderCategoryRepository.findById(id).get();
            return workOrderCategoryRepository.save(workOrderCategoryMapper.updateWorkOrderCategory(saveWorkOrderCategory, workOrderCategory));
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public Collection<WorkOrderCategory> getAll() {
        return workOrderCategoryRepository.findAll();
    }

    public void delete(Long id) {
        workOrderCategoryRepository.deleteById(id);
    }

    public Optional<WorkOrderCategory> findById(Long id) {
        return workOrderCategoryRepository.findById(id);
    }

    public Collection<WorkOrderCategory> findByCompanySettings(Long id) {
        return workOrderCategoryRepository.findByCompanySettings_Id(id);
    }

    public Optional<WorkOrderCategory> findByNameIgnoreCaseAndCompanySettings(String name, Long id) {
        return workOrderCategoryRepository.findByNameIgnoreCaseAndCompanySettings_Id(name, id);
    }
}
