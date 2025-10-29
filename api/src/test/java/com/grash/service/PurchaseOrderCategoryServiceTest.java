package com.grash.service;

import com.grash.dto.CategoryPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.PurchaseOrderCategoryMapper;
import com.grash.model.CompanySettings;
import com.grash.model.PurchaseOrderCategory;
import com.grash.repository.PurchaseOrderCategoryRepository;
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
class PurchaseOrderCategoryServiceTest {

    @Mock
    private PurchaseOrderCategoryRepository purchaseOrderCategoryRepository;

    @Mock
    private PurchaseOrderCategoryMapper purchaseOrderCategoryMapper;

    @InjectMocks
    private PurchaseOrderCategoryService purchaseOrderCategoryService;

    private PurchaseOrderCategory purchaseOrderCategory;
    private CompanySettings companySettings;

    @BeforeEach
    void setUp() {
        companySettings = new CompanySettings();
        companySettings.setId(1L);

        purchaseOrderCategory = new PurchaseOrderCategory();
        purchaseOrderCategory.setId(1L);
        purchaseOrderCategory.setName("Test Category");
        purchaseOrderCategory.setCompanySettings(companySettings);
    }

    @Test
    void create_whenCategoryWithSameNameExists_shouldThrowException() {
        when(purchaseOrderCategoryRepository.findByNameIgnoreCaseAndCompanySettings_Id(anyString(), anyLong()))
                .thenReturn(Optional.of(purchaseOrderCategory));

        CustomException exception = assertThrows(CustomException.class, () -> purchaseOrderCategoryService.create(purchaseOrderCategory));

        assertEquals("PurchaseOrderCategory with same name already exists", exception.getMessage());
        assertEquals(HttpStatus.NOT_ACCEPTABLE, exception.getHttpStatus());
    }

    @Test
    void create_whenCategoryWithSameNameDoesNotExist() {
        when(purchaseOrderCategoryRepository.findByNameIgnoreCaseAndCompanySettings_Id(anyString(), anyLong()))
                .thenReturn(Optional.empty());
        when(purchaseOrderCategoryRepository.save(any(PurchaseOrderCategory.class))).thenReturn(purchaseOrderCategory);

        PurchaseOrderCategory result = purchaseOrderCategoryService.create(purchaseOrderCategory);

        assertNotNull(result);
        assertEquals(purchaseOrderCategory.getId(), result.getId());
        verify(purchaseOrderCategoryRepository).save(purchaseOrderCategory);
    }

    @Test
    void update_whenExists() {
        CategoryPatchDTO patchDTO = new CategoryPatchDTO();
        when(purchaseOrderCategoryRepository.existsById(1L)).thenReturn(true);
        when(purchaseOrderCategoryRepository.findById(1L)).thenReturn(Optional.of(purchaseOrderCategory));
        when(purchaseOrderCategoryRepository.save(any(PurchaseOrderCategory.class))).thenReturn(purchaseOrderCategory);
        when(purchaseOrderCategoryMapper.updatePurchaseOrderCategory(any(PurchaseOrderCategory.class), any(CategoryPatchDTO.class))).thenReturn(purchaseOrderCategory);

        PurchaseOrderCategory result = purchaseOrderCategoryService.update(1L, patchDTO);

        assertNotNull(result);
        assertEquals(purchaseOrderCategory.getId(), result.getId());
        verify(purchaseOrderCategoryRepository).save(purchaseOrderCategory);
    }

    @Test
    void update_whenNotExists_shouldThrowException() {
        CategoryPatchDTO patchDTO = new CategoryPatchDTO();
        when(purchaseOrderCategoryRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class, () -> purchaseOrderCategoryService.update(1L, patchDTO));

        assertEquals("Not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void getAll() {
        when(purchaseOrderCategoryRepository.findAll()).thenReturn(Collections.singletonList(purchaseOrderCategory));

        assertEquals(1, purchaseOrderCategoryService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(purchaseOrderCategoryRepository).deleteById(1L);
        purchaseOrderCategoryService.delete(1L);
        verify(purchaseOrderCategoryRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(purchaseOrderCategoryRepository.findById(1L)).thenReturn(Optional.of(purchaseOrderCategory));

        assertTrue(purchaseOrderCategoryService.findById(1L).isPresent());
    }

    @Test
    void findByCompanySettings() {
        when(purchaseOrderCategoryRepository.findByCompanySettings_Id(1L)).thenReturn(Collections.singletonList(purchaseOrderCategory));

        assertEquals(1, purchaseOrderCategoryService.findByCompanySettings(1L).size());
    }
}
