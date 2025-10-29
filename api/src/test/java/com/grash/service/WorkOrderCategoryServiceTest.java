package com.grash.service;

import com.grash.dto.CategoryPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.WorkOrderCategoryMapper;
import com.grash.model.CompanySettings;
import com.grash.model.WorkOrderCategory;
import com.grash.repository.WorkOrderCategoryRepository;
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
class WorkOrderCategoryServiceTest {

    @Mock
    private WorkOrderCategoryRepository workOrderCategoryRepository;

    @Mock
    private WorkOrderCategoryMapper workOrderCategoryMapper;

    @InjectMocks
    private WorkOrderCategoryService workOrderCategoryService;

    private WorkOrderCategory workOrderCategory;
    private CompanySettings companySettings;

    @BeforeEach
    void setUp() {
        companySettings = new CompanySettings();
        companySettings.setId(1L);

        workOrderCategory = new WorkOrderCategory();
        workOrderCategory.setId(1L);
        workOrderCategory.setName("Test Category");
        workOrderCategory.setCompanySettings(companySettings);
    }

    @Test
    void create_whenCategoryWithSameNameExists_shouldThrowException() {
        when(workOrderCategoryRepository.findByNameIgnoreCaseAndCompanySettings_Id(anyString(), anyLong()))
                .thenReturn(Optional.of(workOrderCategory));

        CustomException exception = assertThrows(CustomException.class, () -> workOrderCategoryService.create(workOrderCategory));

        assertEquals("WorkOrderCategory with same name already exists", exception.getMessage());
        assertEquals(HttpStatus.NOT_ACCEPTABLE, exception.getHttpStatus());
    }

    @Test
    void create_whenCategoryWithSameNameDoesNotExist() {
        when(workOrderCategoryRepository.findByNameIgnoreCaseAndCompanySettings_Id(anyString(), anyLong()))
                .thenReturn(Optional.empty());
        when(workOrderCategoryRepository.save(any(WorkOrderCategory.class))).thenReturn(workOrderCategory);

        WorkOrderCategory result = workOrderCategoryService.create(workOrderCategory);

        assertNotNull(result);
        assertEquals(workOrderCategory.getId(), result.getId());
        verify(workOrderCategoryRepository).save(workOrderCategory);
    }

    @Test
    void update_whenExists() {
        CategoryPatchDTO patchDTO = new CategoryPatchDTO();
        when(workOrderCategoryRepository.existsById(1L)).thenReturn(true);
        when(workOrderCategoryRepository.findById(1L)).thenReturn(Optional.of(workOrderCategory));
        when(workOrderCategoryRepository.save(any(WorkOrderCategory.class))).thenReturn(workOrderCategory);
        when(workOrderCategoryMapper.updateWorkOrderCategory(any(WorkOrderCategory.class), any(CategoryPatchDTO.class))).thenReturn(workOrderCategory);

        WorkOrderCategory result = workOrderCategoryService.update(1L, patchDTO);

        assertNotNull(result);
        assertEquals(workOrderCategory.getId(), result.getId());
        verify(workOrderCategoryRepository).save(workOrderCategory);
    }

    @Test
    void update_whenNotExists_shouldThrowException() {
        CategoryPatchDTO patchDTO = new CategoryPatchDTO();
        when(workOrderCategoryRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class, () -> workOrderCategoryService.update(1L, patchDTO));

        assertEquals("Not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void getAll() {
        when(workOrderCategoryRepository.findAll()).thenReturn(Collections.singletonList(workOrderCategory));

        assertEquals(1, workOrderCategoryService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(workOrderCategoryRepository).deleteById(1L);
        workOrderCategoryService.delete(1L);
        verify(workOrderCategoryRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(workOrderCategoryRepository.findById(1L)).thenReturn(Optional.of(workOrderCategory));

        assertTrue(workOrderCategoryService.findById(1L).isPresent());
    }

    @Test
    void findByCompanySettings() {
        when(workOrderCategoryRepository.findByCompanySettings_Id(1L)).thenReturn(Collections.singletonList(workOrderCategory));

        assertEquals(1, workOrderCategoryService.findByCompanySettings(1L).size());
    }

    @Test
    void findByNameIgnoreCaseAndCompanySettings() {
        when(workOrderCategoryRepository.findByNameIgnoreCaseAndCompanySettings_Id("Test Category", 1L))
                .thenReturn(Optional.of(workOrderCategory));

        assertTrue(workOrderCategoryService.findByNameIgnoreCaseAndCompanySettings("Test Category", 1L).isPresent());
    }
}
