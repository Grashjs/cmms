package com.grash.service;

import com.grash.dto.UiConfigurationPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.UiConfigurationMapper;
import com.grash.model.CompanySettings;
import com.grash.model.UiConfiguration;
import com.grash.repository.UiConfigurationRepository;
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
class UiConfigurationServiceTest {

    @Mock
    private UiConfigurationRepository uiConfigurationRepository;

    @Mock
    private UiConfigurationMapper uiConfigurationMapper;

    @InjectMocks
    private UiConfigurationService uiConfigurationService;

    private UiConfiguration uiConfiguration;
    private CompanySettings companySettings;

    @BeforeEach
    void setUp() {
        companySettings = new CompanySettings();
        companySettings.setId(1L);

        uiConfiguration = new UiConfiguration();
        uiConfiguration.setId(1L);
        uiConfiguration.setCompanySettings(companySettings);
    }

    @Test
    void create() {
        when(uiConfigurationRepository.save(any(UiConfiguration.class))).thenReturn(uiConfiguration);

        UiConfiguration result = uiConfigurationService.create(uiConfiguration);

        assertNotNull(result);
        assertEquals(uiConfiguration.getId(), result.getId());
        verify(uiConfigurationRepository).save(uiConfiguration);
    }

    @Test
    void update_whenExists() {
        UiConfigurationPatchDTO patchDTO = new UiConfigurationPatchDTO();
        when(uiConfigurationRepository.existsById(1L)).thenReturn(true);
        when(uiConfigurationRepository.findById(1L)).thenReturn(Optional.of(uiConfiguration));
        when(uiConfigurationRepository.save(any(UiConfiguration.class))).thenReturn(uiConfiguration);
        when(uiConfigurationMapper.updateUiConfiguration(any(UiConfiguration.class), any(UiConfigurationPatchDTO.class))).thenReturn(uiConfiguration);

        UiConfiguration result = uiConfigurationService.update(1L, patchDTO);

        assertNotNull(result);
        assertEquals(uiConfiguration.getId(), result.getId());
        verify(uiConfigurationRepository).save(uiConfiguration);
    }

    @Test
    void update_whenNotExists_shouldThrowException() {
        UiConfigurationPatchDTO patchDTO = new UiConfigurationPatchDTO();
        when(uiConfigurationRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class, () -> uiConfigurationService.update(1L, patchDTO));

        assertEquals("Not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void getAll() {
        when(uiConfigurationRepository.findAll()).thenReturn(Collections.singletonList(uiConfiguration));

        assertEquals(1, uiConfigurationService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(uiConfigurationRepository).deleteById(1L);
        uiConfigurationService.delete(1L);
        verify(uiConfigurationRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(uiConfigurationRepository.findById(1L)).thenReturn(Optional.of(uiConfiguration));

        assertTrue(uiConfigurationService.findById(1L).isPresent());
    }

    @Test
    void findByCompanySettings() {
        when(uiConfigurationRepository.findByCompanySettings_Id(1L)).thenReturn(Optional.of(uiConfiguration));

        assertTrue(uiConfigurationService.findByCompanySettings(1L).isPresent());
    }
}
