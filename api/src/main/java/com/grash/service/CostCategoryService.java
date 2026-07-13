package com.grash.service;

import com.grash.dto.CategoryPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.CostCategoryMapper;
import com.grash.model.CostCategory;
import com.grash.model.OwnUser;
import com.grash.model.enums.RoleType;
import com.grash.repository.CostCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CostCategoryService {
    private final CostCategoryRepository costCategoryRepository;

    private final CostCategoryMapper costCategoryMapper;

    public CostCategory create(CostCategory costCategory) {
        Optional<CostCategory> categoryWithSameName = costCategoryRepository.findByNameIgnoreCaseAndCompanySettings_Id(costCategory.getName(), costCategory.getCompanySettings().getId());
        if (categoryWithSameName.isPresent()) {
            throw new CustomException("CostCategory with same name already exists", HttpStatus.NOT_ACCEPTABLE);
        }
        return costCategoryRepository.save(costCategory);
    }

    public CostCategory update(Long id, CategoryPatchDTO costCategory) {
        if (costCategoryRepository.existsById(id)) {
            CostCategory savedCostCategory = costCategoryRepository.findById(id).get();
            return costCategoryRepository.save(costCategoryMapper.updateCostCategory(savedCostCategory, costCategory));
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);

    }

    public Collection<CostCategory> getAll() {
        return costCategoryRepository.findAll();
    }

    public void delete(Long id) {
        costCategoryRepository.deleteById(id);
    }

    public Optional<CostCategory> findById(Long id) {
        return costCategoryRepository.findById(id);
    }

    public Collection<CostCategory> findByCompanySettings(Long id) {
        return costCategoryRepository.findByCompanySettings_Id(id);
    }
}
