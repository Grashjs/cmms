package com.grash.service;

import com.grash.model.Company;
import com.grash.model.CustomSequence;
import com.grash.repository.CustomSequenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomSequenceServiceTest {

    @Mock
    private CustomSequenceRepository customSequenceRepository;

    @InjectMocks
    private CustomSequenceService customSequenceService;

    private Company company;
    private CustomSequence customSequence;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);

        customSequence = new CustomSequence(company);
    }

    @Test
    void findByCompanyId() {
        when(customSequenceRepository.findByCompanyId(1L)).thenReturn(Optional.of(customSequence));

        CustomSequence result = customSequenceService.findByCompanyId(1L);

        assertNotNull(result);
        assertEquals(customSequence, result);
    }

    @Test
    void createCustomSequence() {
        when(customSequenceRepository.save(any(CustomSequence.class))).thenReturn(customSequence);

        CustomSequence result = customSequenceService.createCustomSequence(company);

        assertNotNull(result);
        assertEquals(customSequence, result);
    }

    @Test
    void getOrCreateCustomSequence_whenExists() {
        when(customSequenceRepository.findByCompanyId(1L)).thenReturn(Optional.of(customSequence));

        CustomSequence result = customSequenceService.getOrCreateCustomSequence(company);

        assertNotNull(result);
        assertEquals(customSequence, result);
    }

    @Test
    void getOrCreateCustomSequence_whenNotExists() {
        when(customSequenceRepository.findByCompanyId(1L)).thenReturn(Optional.empty());
        when(customSequenceRepository.save(any(CustomSequence.class))).thenReturn(customSequence);

        CustomSequence result = customSequenceService.getOrCreateCustomSequence(company);

        assertNotNull(result);
        assertEquals(customSequence, result);
    }

    @Test
    void getNextWorkOrderSequence() {
        when(customSequenceRepository.findByCompanyId(1L)).thenReturn(Optional.of(customSequence));
        when(customSequenceRepository.save(any(CustomSequence.class))).thenReturn(customSequence);

        Long result = customSequenceService.getNextWorkOrderSequence(company);

        assertEquals(1L, result);
        assertEquals(2L, customSequence.getWorkOrderSequence());
    }

    @Test
    void getNextAssetSequence() {
        when(customSequenceRepository.findByCompanyId(1L)).thenReturn(Optional.of(customSequence));
        when(customSequenceRepository.save(any(CustomSequence.class))).thenReturn(customSequence);

        Long result = customSequenceService.getNextAssetSequence(company);

        assertEquals(1L, result);
        assertEquals(2L, customSequence.getAssetSequence());
    }

    @Test
    void getNextPreventiveMaintenanceSequence() {
        when(customSequenceRepository.findByCompanyId(1L)).thenReturn(Optional.of(customSequence));
        when(customSequenceRepository.save(any(CustomSequence.class))).thenReturn(customSequence);

        Long result = customSequenceService.getNextPreventiveMaintenanceSequence(company);

        assertEquals(1L, result);
        assertEquals(2L, customSequence.getPreventiveMaintenanceSequence());
    }

    @Test
    void getNextLocationSequence() {
        when(customSequenceRepository.findByCompanyId(1L)).thenReturn(Optional.of(customSequence));
        when(customSequenceRepository.save(any(CustomSequence.class))).thenReturn(customSequence);

        Long result = customSequenceService.getNextLocationSequence(company);

        assertEquals(1L, result);
        assertEquals(2L, customSequence.getLocationSequence());
    }

    @Test
    void getNextRequestSequence() {
        when(customSequenceRepository.findByCompanyId(1L)).thenReturn(Optional.of(customSequence));
        when(customSequenceRepository.save(any(CustomSequence.class))).thenReturn(customSequence);

        Long result = customSequenceService.getNextRequestSequence(company);

        assertEquals(1L, result);
        assertEquals(2L, customSequence.getRequestSequence());
    }
}
