package com.grash.service;

import com.grash.dto.CategoryPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.MeterCategoryMapper;
import com.grash.model.Company;
import com.grash.model.CompanySettings;
import com.grash.model.MeterCategory;
import com.grash.repository.MeterCategoryRepository;
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
class MeterCategoryServiceTest {

    @Mock
    private MeterCategoryRepository meterCategoryRepository;

    @Mock
    private MeterCategoryMapper meterCategoryMapper;

    @InjectMocks
    private MeterCategoryService meterCategoryService;

    private MeterCategory meterCategory;
    private CompanySettings companySettings;
    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);

        companySettings = new CompanySettings();
        companySettings.setId(1L);
        companySettings.setCompany(company);

        meterCategory = new MeterCategory();
        meterCategory.setId(1L);
        meterCategory.setName("Test Category");
        meterCategory.setCompanySettings(companySettings);
    }

    @Test
    void create_whenCategoryWithSameNameExists_shouldThrowException() {
        when(meterCategoryRepository.findByNameIgnoreCaseAndCompanySettings_Id(anyString(), anyLong()))
                .thenReturn(Optional.of(meterCategory));

        CustomException exception = assertThrows(CustomException.class, () -> meterCategoryService.create(meterCategory));

        assertEquals("MeterCategory with same name already exists", exception.getMessage());
        assertEquals(HttpStatus.NOT_ACCEPTABLE, exception.getHttpStatus());
    }

    @Test
    void create_whenCategoryWithSameNameDoesNotExist() {
        when(meterCategoryRepository.findByNameIgnoreCaseAndCompanySettings_Id(anyString(), anyLong()))
                .thenReturn(Optional.empty());
        when(meterCategoryRepository.save(any(MeterCategory.class))).thenReturn(meterCategory);

        MeterCategory result = meterCategoryService.create(meterCategory);

        assertNotNull(result);
        assertEquals(meterCategory.getId(), result.getId());
        verify(meterCategoryRepository).save(meterCategory);
    }

    @Test
    void update_whenExists() {
        CategoryPatchDTO patchDTO = new CategoryPatchDTO();
        when(meterCategoryRepository.existsById(1L)).thenReturn(true);
        when(meterCategoryRepository.findById(1L)).thenReturn(Optional.of(meterCategory));
        when(meterCategoryRepository.save(any(MeterCategory.class))).thenReturn(meterCategory);
        when(meterCategoryMapper.updateMeterCategory(any(MeterCategory.class), any(CategoryPatchDTO.class))).thenReturn(meterCategory);

        MeterCategory result = meterCategoryService.update(1L, patchDTO);

        assertNotNull(result);
        assertEquals(meterCategory.getId(), result.getId());
        verify(meterCategoryRepository).save(meterCategory);
    }

    @Test
    void update_whenNotExists_shouldThrowException() {
        CategoryPatchDTO patchDTO = new CategoryPatchDTO();
        when(meterCategoryRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class, () -> meterCategoryService.update(1L, patchDTO));

        assertEquals("Not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void getAll() {
        when(meterCategoryRepository.findAll()).thenReturn(Collections.singletonList(meterCategory));

        assertEquals(1, meterCategoryService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(meterCategoryRepository).deleteById(1L);
        meterCategoryService.delete(1L);
        verify(meterCategoryRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(meterCategoryRepository.findById(1L)).thenReturn(Optional.of(meterCategory));

        assertTrue(meterCategoryService.findById(1L).isPresent());
    }

    @Test
    void findByCompany() {
        when(meterCategoryRepository.findByCompany_Id(1L)).thenReturn(Collections.singletonList(meterCategory));

        assertEquals(1, meterCategoryService.findByCompany(1L).size());
    }

    @Test
    void findByNameIgnoreCaseAndCompanySettings() {
        when(meterCategoryRepository.findByNameIgnoreCaseAndCompanySettings_Id("Test Category", 1L))
                .thenReturn(Optional.of(meterCategory));

        assertTrue(meterCategoryService.findByNameIgnoreCaseAndCompanySettings("Test Category", 1L).isPresent());
    }
}
