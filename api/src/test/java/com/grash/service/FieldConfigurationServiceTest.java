package com.grash.service;

import com.grash.dto.FieldConfigurationPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.FieldConfigurationMapper;
import com.grash.model.FieldConfiguration;
import com.grash.repository.FieldConfigurationRepository;
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
class FieldConfigurationServiceTest {

    @Mock
    private FieldConfigurationRepository fieldConfigurationRepository;

    @Mock
    private FieldConfigurationMapper fieldConfigurationMapper;

    @InjectMocks
    private FieldConfigurationService fieldConfigurationService;

    private FieldConfiguration fieldConfiguration;

    @BeforeEach
    void setUp() {
        fieldConfiguration = new FieldConfiguration();
        fieldConfiguration.setId(1L);
    }

    @Test
    void create() {
        when(fieldConfigurationRepository.save(any(FieldConfiguration.class))).thenReturn(fieldConfiguration);

        FieldConfiguration result = fieldConfigurationService.create(fieldConfiguration);

        assertNotNull(result);
        assertEquals(fieldConfiguration.getId(), result.getId());
        verify(fieldConfigurationRepository).save(fieldConfiguration);
    }

    @Test
    void update_whenExists() {
        FieldConfigurationPatchDTO patchDTO = new FieldConfigurationPatchDTO();
        when(fieldConfigurationRepository.existsById(1L)).thenReturn(true);
        when(fieldConfigurationRepository.findById(1L)).thenReturn(Optional.of(fieldConfiguration));
        when(fieldConfigurationRepository.save(any(FieldConfiguration.class))).thenReturn(fieldConfiguration);
        when(fieldConfigurationMapper.updateFieldConfiguration(any(FieldConfiguration.class), any(FieldConfigurationPatchDTO.class))).thenReturn(fieldConfiguration);

        FieldConfiguration result = fieldConfigurationService.update(1L, patchDTO);

        assertNotNull(result);
        assertEquals(fieldConfiguration.getId(), result.getId());
        verify(fieldConfigurationRepository).save(fieldConfiguration);
    }

    @Test
    void update_whenNotExists_shouldThrowException() {
        FieldConfigurationPatchDTO patchDTO = new FieldConfigurationPatchDTO();
        when(fieldConfigurationRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class, () -> fieldConfigurationService.update(1L, patchDTO));

        assertEquals("Not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void getAll() {
        when(fieldConfigurationRepository.findAll()).thenReturn(Collections.singletonList(fieldConfiguration));

        assertEquals(1, fieldConfigurationService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(fieldConfigurationRepository).deleteById(1L);
        fieldConfigurationService.delete(1L);
        verify(fieldConfigurationRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(fieldConfigurationRepository.findById(1L)).thenReturn(Optional.of(fieldConfiguration));

        assertTrue(fieldConfigurationService.findById(1L).isPresent());
    }
}
