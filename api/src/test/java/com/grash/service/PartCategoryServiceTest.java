package com.grash.service;

import com.grash.dto.CategoryPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.PartCategoryMapper;
import com.grash.model.CompanySettings;
import com.grash.model.PartCategory;
import com.grash.repository.PartCategoryRepository;
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
class PartCategoryServiceTest {

    @Mock
    private PartCategoryRepository partCategoryRepository;

    @Mock
    private PartCategoryMapper partCategoryMapper;

    @InjectMocks
    private PartCategoryService partCategoryService;

    private PartCategory partCategory;
    private CompanySettings companySettings;

    @BeforeEach
    void setUp() {
        companySettings = new CompanySettings();
        companySettings.setId(1L);

        partCategory = new PartCategory();
        partCategory.setId(1L);
        partCategory.setName("Test Category");
        partCategory.setCompanySettings(companySettings);
    }

    @Test
    void create_whenCategoryWithSameNameExists_shouldThrowException() {
        when(partCategoryRepository.findByNameIgnoreCaseAndCompanySettings_Id(anyString(), anyLong()))
                .thenReturn(Optional.of(partCategory));

        CustomException exception = assertThrows(CustomException.class, () -> partCategoryService.create(partCategory));

        assertEquals("PartCategory with same name already exists", exception.getMessage());
        assertEquals(HttpStatus.NOT_ACCEPTABLE, exception.getHttpStatus());
    }

    @Test
    void create_whenCategoryWithSameNameDoesNotExist() {
        when(partCategoryRepository.findByNameIgnoreCaseAndCompanySettings_Id(anyString(), anyLong()))
                .thenReturn(Optional.empty());
        when(partCategoryRepository.save(any(PartCategory.class))).thenReturn(partCategory);

        PartCategory result = partCategoryService.create(partCategory);

        assertNotNull(result);
        assertEquals(partCategory.getId(), result.getId());
        verify(partCategoryRepository).save(partCategory);
    }

    @Test
    void update_whenExists() {
        CategoryPatchDTO patchDTO = new CategoryPatchDTO();
        when(partCategoryRepository.existsById(1L)).thenReturn(true);
        when(partCategoryRepository.findById(1L)).thenReturn(Optional.of(partCategory));
        when(partCategoryRepository.save(any(PartCategory.class))).thenReturn(partCategory);
        when(partCategoryMapper.updatePartCategory(any(PartCategory.class), any(CategoryPatchDTO.class))).thenReturn(partCategory);

        PartCategory result = partCategoryService.update(1L, patchDTO);

        assertNotNull(result);
        assertEquals(partCategory.getId(), result.getId());
        verify(partCategoryRepository).save(partCategory);
    }

    @Test
    void update_whenNotExists_shouldThrowException() {
        CategoryPatchDTO patchDTO = new CategoryPatchDTO();
        when(partCategoryRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class, () -> partCategoryService.update(1L, patchDTO));

        assertEquals("Not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void getAll() {
        when(partCategoryRepository.findAll()).thenReturn(Collections.singletonList(partCategory));

        assertEquals(1, partCategoryService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(partCategoryRepository).deleteById(1L);
        partCategoryService.delete(1L);
        verify(partCategoryRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(partCategoryRepository.findById(1L)).thenReturn(Optional.of(partCategory));

        assertTrue(partCategoryService.findById(1L).isPresent());
    }

    @Test
    void findByCompanySettings() {
        when(partCategoryRepository.findByCompanySettings_Id(1L)).thenReturn(Collections.singletonList(partCategory));

        assertEquals(1, partCategoryService.findByCompanySettings(1L).size());
    }

    @Test
    void findByNameIgnoreCaseAndCompanySettings() {
        when(partCategoryRepository.findByNameIgnoreCaseAndCompanySettings_Id("Test Category", 1L))
                .thenReturn(Optional.of(partCategory));

        assertTrue(partCategoryService.findByNameIgnoreCaseAndCompanySettings("Test Category", 1L).isPresent());
    }
}
