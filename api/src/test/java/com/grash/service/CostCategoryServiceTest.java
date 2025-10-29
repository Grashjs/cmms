package com.grash.service;

import com.grash.dto.CategoryPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.CostCategoryMapper;
import com.grash.model.CostCategory;
import com.grash.model.CompanySettings;
import com.grash.repository.CostCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CostCategoryServiceTest {

    @Mock
    private CostCategoryRepository costCategoryRepository;

    @Mock
    private CostCategoryMapper costCategoryMapper;

    @InjectMocks
    private CostCategoryService costCategoryService;

    private CostCategory costCategory;
    private CompanySettings companySettings;

    @BeforeEach
    void setUp() {
        companySettings = new CompanySettings();
        companySettings.setId(1L);

        costCategory = new CostCategory();
        costCategory.setId(1L);
        costCategory.setName("Test Category");
        costCategory.setCompanySettings(companySettings);
    }

    @Test
    void create_whenCategoryWithSameNameExists_shouldThrowException() {
        when(costCategoryRepository.findByNameIgnoreCaseAndCompanySettings_Id(anyString(), anyLong()))
                .thenReturn(Optional.of(costCategory));

        CustomException exception = assertThrows(CustomException.class, () -> costCategoryService.create(costCategory));

        assertEquals("CostCategory with same name already exists", exception.getMessage());
        assertEquals(HttpStatus.NOT_ACCEPTABLE, exception.getHttpStatus());
    }

    @Test
    void create_whenCategoryWithSameNameDoesNotExist() {
        when(costCategoryRepository.findByNameIgnoreCaseAndCompanySettings_Id(anyString(), anyLong()))
                .thenReturn(Optional.empty());
        when(costCategoryRepository.save(any(CostCategory.class))).thenReturn(costCategory);

        CostCategory result = costCategoryService.create(costCategory);

        assertNotNull(result);
        assertEquals(costCategory.getId(), result.getId());
        verify(costCategoryRepository).save(costCategory);
    }

    @Test
    void update_whenExists() {
        CategoryPatchDTO patchDTO = new CategoryPatchDTO();
        when(costCategoryRepository.existsById(1L)).thenReturn(true);
        when(costCategoryRepository.findById(1L)).thenReturn(Optional.of(costCategory));
        when(costCategoryRepository.save(any(CostCategory.class))).thenReturn(costCategory);
        when(costCategoryMapper.updateCostCategory(any(CostCategory.class), any(CategoryPatchDTO.class))).thenReturn(costCategory);

        CostCategory result = costCategoryService.update(1L, patchDTO);

        assertNotNull(result);
        assertEquals(costCategory.getId(), result.getId());
        verify(costCategoryRepository).save(costCategory);
    }

    @Test
    void update_whenNotExists_shouldThrowException() {
        CategoryPatchDTO patchDTO = new CategoryPatchDTO();
        when(costCategoryRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class, () -> costCategoryService.update(1L, patchDTO));

        assertEquals("Not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void getAll() {
        when(costCategoryRepository.findAll()).thenReturn(Collections.singletonList(costCategory));

        assertEquals(1, costCategoryService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(costCategoryRepository).deleteById(1L);
        costCategoryService.delete(1L);
        verify(costCategoryRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(costCategoryRepository.findById(1L)).thenReturn(Optional.of(costCategory));

        assertTrue(costCategoryService.findById(1L).isPresent());
    }

    @Test
    void findByCompanySettings() {
        when(costCategoryRepository.findByCompanySettings_Id(1L)).thenReturn(Collections.singletonList(costCategory));

        assertEquals(1, costCategoryService.findByCompanySettings(1L).size());
    }
}
