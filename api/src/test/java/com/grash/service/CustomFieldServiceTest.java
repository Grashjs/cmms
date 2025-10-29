package com.grash.service;

import com.grash.dto.CustomFieldPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.CustomFieldMapper;
import com.grash.model.CustomField;
import com.grash.repository.CustomFieldRepository;
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
class CustomFieldServiceTest {

    @Mock
    private CustomFieldRepository customFieldRepository;

    @Mock
    private CustomFieldMapper customFieldMapper;

    @InjectMocks
    private CustomFieldService customFieldService;

    private CustomField customField;

    @BeforeEach
    void setUp() {
        customField = new CustomField();
        customField.setId(1L);
    }

    @Test
    void create() {
        when(customFieldRepository.save(any(CustomField.class))).thenReturn(customField);

        CustomField result = customFieldService.create(customField);

        assertNotNull(result);
        assertEquals(customField.getId(), result.getId());
        verify(customFieldRepository).save(customField);
    }

    @Test
    void update_whenExists() {
        CustomFieldPatchDTO patchDTO = new CustomFieldPatchDTO();
        when(customFieldRepository.existsById(1L)).thenReturn(true);
        when(customFieldRepository.findById(1L)).thenReturn(Optional.of(customField));
        when(customFieldRepository.save(any(CustomField.class))).thenReturn(customField);
        when(customFieldMapper.updateCustomField(any(CustomField.class), any(CustomFieldPatchDTO.class))).thenReturn(customField);

        CustomField result = customFieldService.update(1L, patchDTO);

        assertNotNull(result);
        assertEquals(customField.getId(), result.getId());
        verify(customFieldRepository).save(customField);
    }

    @Test
    void update_whenNotExists_shouldThrowException() {
        CustomFieldPatchDTO patchDTO = new CustomFieldPatchDTO();
        when(customFieldRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class, () -> customFieldService.update(1L, patchDTO));

        assertEquals("Not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void getAll() {
        when(customFieldRepository.findAll()).thenReturn(Collections.singletonList(customField));

        assertEquals(1, customFieldService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(customFieldRepository).deleteById(1L);
        customFieldService.delete(1L);
        verify(customFieldRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(customFieldRepository.findById(1L)).thenReturn(Optional.of(customField));

        assertTrue(customFieldService.findById(1L).isPresent());
    }
}
