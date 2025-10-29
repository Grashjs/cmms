package com.grash.service;

import com.grash.model.CompanySettings;
import com.grash.repository.CompanySettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanySettingsServiceTest {

    @Mock
    private CompanySettingsRepository companySettingsRepository;

    @InjectMocks
    private CompanySettingsService companySettingsService;

    private CompanySettings companySettings;

    @BeforeEach
    void setUp() {
        companySettings = new CompanySettings();
        companySettings.setId(1L);
    }

    @Test
    void create() {
        when(companySettingsRepository.save(any(CompanySettings.class))).thenReturn(companySettings);

        CompanySettings result = companySettingsService.create(companySettings);

        assertNotNull(result);
        assertEquals(companySettings.getId(), result.getId());
        verify(companySettingsRepository).save(companySettings);
    }

    @Test
    void update() {
        when(companySettingsRepository.save(any(CompanySettings.class))).thenReturn(companySettings);

        CompanySettings result = companySettingsService.update(companySettings);

        assertNotNull(result);
        assertEquals(companySettings.getId(), result.getId());
        verify(companySettingsRepository).save(companySettings);
    }

    @Test
    void getAll() {
        Pageable pageable = Pageable.unpaged();
        Page<CompanySettings> page = new PageImpl<>(Collections.singletonList(companySettings));
        when(companySettingsRepository.findAll(pageable)).thenReturn(page);

        assertEquals(1, companySettingsService.getAll(pageable).getTotalElements());
    }

    @Test
    void delete() {
        doNothing().when(companySettingsRepository).deleteById(1L);
        companySettingsService.delete(1L);
        verify(companySettingsRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(companySettingsRepository.findById(1L)).thenReturn(Optional.of(companySettings));

        assertTrue(companySettingsService.findById(1L).isPresent());
    }
}
