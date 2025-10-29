package com.grash.service;

import com.grash.dto.CategoryPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.AssetCategoryMapper;
import com.grash.model.AssetCategory;
import com.grash.model.Company;
import com.grash.model.CompanySettings;
import com.grash.repository.AssetCategoryRepository;
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
class AssetCategoryServiceTest {

    @Mock
    private AssetCategoryRepository assetCategoryRepository;

    @Mock
    private AssetCategoryMapper assetCategoryMapper;

    @InjectMocks
    private AssetCategoryService assetCategoryService;

    private AssetCategory assetCategory;
    private CompanySettings companySettings;

    @BeforeEach
    void setUp() {
        Company company = new Company();
        company.setId(1L);

        companySettings = new CompanySettings();
        companySettings.setId(1L);
        companySettings.setCompany(company);

        assetCategory = new AssetCategory();
        assetCategory.setId(1L);
        assetCategory.setName("Test Category");
        assetCategory.setCompanySettings(companySettings);
    }

    @Test
    void create_whenCategoryWithSameNameExists_shouldThrowException() {
        when(assetCategoryRepository.findByNameIgnoreCaseAndCompanySettings_Id(anyString(), anyLong()))
                .thenReturn(Optional.of(assetCategory));

        CustomException exception = assertThrows(CustomException.class, () -> assetCategoryService.create(assetCategory));

        assertEquals("AssetCategory with same name already exists", exception.getMessage());
        assertEquals(HttpStatus.NOT_ACCEPTABLE, exception.getHttpStatus());
    }

    @Test
    void create_whenCategoryWithSameNameDoesNotExist() {
        when(assetCategoryRepository.findByNameIgnoreCaseAndCompanySettings_Id(anyString(), anyLong()))
                .thenReturn(Optional.empty());
        when(assetCategoryRepository.save(any(AssetCategory.class))).thenReturn(assetCategory);

        AssetCategory result = assetCategoryService.create(assetCategory);

        assertNotNull(result);
        assertEquals(assetCategory.getId(), result.getId());
        verify(assetCategoryRepository).save(assetCategory);
    }

    @Test
    void update_whenExists() {
        CategoryPatchDTO patchDTO = new CategoryPatchDTO();
        when(assetCategoryRepository.existsById(1L)).thenReturn(true);
        when(assetCategoryRepository.findById(1L)).thenReturn(Optional.of(assetCategory));
        when(assetCategoryRepository.save(any(AssetCategory.class))).thenReturn(assetCategory);
        when(assetCategoryMapper.updateAssetCategory(any(AssetCategory.class), any(CategoryPatchDTO.class))).thenReturn(assetCategory);

        AssetCategory result = assetCategoryService.update(1L, patchDTO);

        assertNotNull(result);
        assertEquals(assetCategory.getId(), result.getId());
        verify(assetCategoryRepository).save(assetCategory);
    }

    @Test
    void update_whenNotExists_shouldThrowException() {
        CategoryPatchDTO patchDTO = new CategoryPatchDTO();
        when(assetCategoryRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class, () -> assetCategoryService.update(1L, patchDTO));

        assertEquals("Not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void getAll() {
        when(assetCategoryRepository.findAll()).thenReturn(Collections.singletonList(assetCategory));

        assertEquals(1, assetCategoryService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(assetCategoryRepository).deleteById(1L);
        assetCategoryService.delete(1L);
        verify(assetCategoryRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(assetCategoryRepository.findById(1L)).thenReturn(Optional.of(assetCategory));

        assertTrue(assetCategoryService.findById(1L).isPresent());
    }

    @Test
    void findByCompanySettings() {
        when(assetCategoryRepository.findByCompanySettings_Id(1L)).thenReturn(Collections.singletonList(assetCategory));

        assertEquals(1, assetCategoryService.findByCompanySettings(1L).size());
    }

    @Test
    void isAssetCategoryInCompany_whenOptionalAndNull() {
        assertTrue(assetCategoryService.isAssetCategoryInCompany(null, 1L, true));
    }

    @Test
    void isAssetCategoryInCompany_whenOptionalAndNotNullAndInCompany() {
        when(assetCategoryRepository.findById(1L)).thenReturn(Optional.of(assetCategory));
        assertTrue(assetCategoryService.isAssetCategoryInCompany(assetCategory, 1L, true));
    }

    @Test
    void isAssetCategoryInCompany_whenOptionalAndNotNullAndNotInCompany() {
        when(assetCategoryRepository.findById(1L)).thenReturn(Optional.of(assetCategory));
        assertFalse(assetCategoryService.isAssetCategoryInCompany(assetCategory, 2L, true));
    }

    @Test
    void isAssetCategoryInCompany_whenNotOptionalAndInCompany() {
        when(assetCategoryRepository.findById(1L)).thenReturn(Optional.of(assetCategory));
        assertTrue(assetCategoryService.isAssetCategoryInCompany(assetCategory, 1L, false));
    }

    @Test
    void isAssetCategoryInCompany_whenNotOptionalAndNotInCompany() {
        when(assetCategoryRepository.findById(1L)).thenReturn(Optional.of(assetCategory));
        assertFalse(assetCategoryService.isAssetCategoryInCompany(assetCategory, 2L, false));
    }

    @Test
    void findByNameIgnoreCaseAndCompanySettings() {
        when(assetCategoryRepository.findByNameIgnoreCaseAndCompanySettings_Id("Test Category", 1L))
                .thenReturn(Optional.of(assetCategory));

        assertTrue(assetCategoryService.findByNameIgnoreCaseAndCompanySettings("Test Category", 1L).isPresent());
    }

    @Test
    void isAssetCategoryInCompany_whenOptionalAndNotNullAndNotInRepo() {
        when(assetCategoryRepository.findById(1L)).thenReturn(Optional.empty());
        assertFalse(assetCategoryService.isAssetCategoryInCompany(assetCategory, 1L, true));
    }
}
