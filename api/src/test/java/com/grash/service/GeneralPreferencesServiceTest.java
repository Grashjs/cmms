package com.grash.service;

import com.grash.dto.GeneralPreferencesPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.GeneralPreferencesMapper;
import com.grash.model.CompanySettings;
import com.grash.model.GeneralPreferences;
import com.grash.repository.GeneralPreferencesRepository;
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
class GeneralPreferencesServiceTest {

    @Mock
    private GeneralPreferencesRepository generalPreferencesRepository;

    @Mock
    private GeneralPreferencesMapper generalPreferencesMapper;

    @InjectMocks
    private GeneralPreferencesService generalPreferencesService;

    private GeneralPreferences generalPreferences;
    private CompanySettings companySettings;

    @BeforeEach
    void setUp() {
        companySettings = new CompanySettings();
        companySettings.setId(1L);

        generalPreferences = new GeneralPreferences();
        generalPreferences.setId(1L);
        generalPreferences.setCompanySettings(companySettings);
    }

    @Test
    void create() {
        when(generalPreferencesRepository.save(any(GeneralPreferences.class))).thenReturn(generalPreferences);

        GeneralPreferences result = generalPreferencesService.create(generalPreferences);

        assertNotNull(result);
        assertEquals(generalPreferences.getId(), result.getId());
        verify(generalPreferencesRepository).save(generalPreferences);
    }

    @Test
    void update_whenExists() {
        GeneralPreferencesPatchDTO patchDTO = new GeneralPreferencesPatchDTO();
        when(generalPreferencesRepository.existsById(1L)).thenReturn(true);
        when(generalPreferencesRepository.findById(1L)).thenReturn(Optional.of(generalPreferences));
        when(generalPreferencesRepository.save(any(GeneralPreferences.class))).thenReturn(generalPreferences);
        when(generalPreferencesMapper.updateGeneralPreferences(any(GeneralPreferences.class), any(GeneralPreferencesPatchDTO.class))).thenReturn(generalPreferences);

        GeneralPreferences result = generalPreferencesService.update(1L, patchDTO);

        assertNotNull(result);
        assertEquals(generalPreferences.getId(), result.getId());
        verify(generalPreferencesRepository).save(generalPreferences);
    }

    @Test
    void update_whenNotExists_shouldThrowException() {
        GeneralPreferencesPatchDTO patchDTO = new GeneralPreferencesPatchDTO();
        when(generalPreferencesRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class, () -> generalPreferencesService.update(1L, patchDTO));

        assertEquals("Not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void getAll() {
        when(generalPreferencesRepository.findAll()).thenReturn(Collections.singletonList(generalPreferences));

        assertEquals(1, generalPreferencesService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(generalPreferencesRepository).deleteById(1L);
        generalPreferencesService.delete(1L);
        verify(generalPreferencesRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(generalPreferencesRepository.findById(1L)).thenReturn(Optional.of(generalPreferences));

        assertTrue(generalPreferencesService.findById(1L).isPresent());
    }

    @Test
    void findByCompanySettings() {
        when(generalPreferencesRepository.findByCompanySettings_Id(1L)).thenReturn(Collections.singletonList(generalPreferences));

        assertEquals(1, generalPreferencesService.findByCompanySettings(1L).size());
    }
}
