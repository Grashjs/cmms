package com.grash.service;

import com.grash.dto.CategoryPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.TimeCategoryMapper;
import com.grash.model.Company;
import com.grash.model.CompanySettings;
import com.grash.model.TimeCategory;
import com.grash.repository.TimeCategoryRepository;
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
class TimeCategoryServiceTest {

    @Mock
    private TimeCategoryRepository timeCategoryRepository;

    @Mock
    private TimeCategoryMapper timeCategoryMapper;

    @InjectMocks
    private TimeCategoryService timeCategoryService;

    private TimeCategory timeCategory;
    private CompanySettings companySettings;
    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);

        companySettings = new CompanySettings();
        companySettings.setId(1L);
        companySettings.setCompany(company);

        timeCategory = new TimeCategory();
        timeCategory.setId(1L);
        timeCategory.setName("Test Category");
        timeCategory.setCompanySettings(companySettings);
    }

    @Test
    void create_whenCategoryWithSameNameExists_shouldThrowException() {
        when(timeCategoryRepository.findByNameIgnoreCaseAndCompanySettings_Id(anyString(), anyLong()))
                .thenReturn(Optional.of(timeCategory));

        CustomException exception = assertThrows(CustomException.class, () -> timeCategoryService.create(timeCategory));

        assertEquals("TimeCategory with same name already exists", exception.getMessage());
        assertEquals(HttpStatus.NOT_ACCEPTABLE, exception.getHttpStatus());
    }

    @Test
    void create_whenCategoryWithSameNameDoesNotExist() {
        when(timeCategoryRepository.findByNameIgnoreCaseAndCompanySettings_Id(anyString(), anyLong()))
                .thenReturn(Optional.empty());
        when(timeCategoryRepository.save(any(TimeCategory.class))).thenReturn(timeCategory);

        TimeCategory result = timeCategoryService.create(timeCategory);

        assertNotNull(result);
        assertEquals(timeCategory.getId(), result.getId());
        verify(timeCategoryRepository).save(timeCategory);
    }

    @Test
    void update_whenExists() {
        CategoryPatchDTO patchDTO = new CategoryPatchDTO();
        when(timeCategoryRepository.existsById(1L)).thenReturn(true);
        when(timeCategoryRepository.findById(1L)).thenReturn(Optional.of(timeCategory));
        when(timeCategoryRepository.save(any(TimeCategory.class))).thenReturn(timeCategory);
        when(timeCategoryMapper.updateTimeCategory(any(TimeCategory.class), any(CategoryPatchDTO.class))).thenReturn(timeCategory);

        TimeCategory result = timeCategoryService.update(1L, patchDTO);

        assertNotNull(result);
        assertEquals(timeCategory.getId(), result.getId());
        verify(timeCategoryRepository).save(timeCategory);
    }

    @Test
    void update_whenNotExists_shouldThrowException() {
        CategoryPatchDTO patchDTO = new CategoryPatchDTO();
        when(timeCategoryRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class, () -> timeCategoryService.update(1L, patchDTO));

        assertEquals("Not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void getAll() {
        when(timeCategoryRepository.findAll()).thenReturn(Collections.singletonList(timeCategory));

        assertEquals(1, timeCategoryService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(timeCategoryRepository).deleteById(1L);
        timeCategoryService.delete(1L);
        verify(timeCategoryRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(timeCategoryRepository.findById(1L)).thenReturn(Optional.of(timeCategory));

        assertTrue(timeCategoryService.findById(1L).isPresent());
    }

    @Test
    void findByCompanySettings() {
        when(timeCategoryRepository.findByCompanySettings_Id(1L)).thenReturn(Collections.singletonList(timeCategory));

        assertEquals(1, timeCategoryService.findByCompanySettings(1L).size());
    }
}
