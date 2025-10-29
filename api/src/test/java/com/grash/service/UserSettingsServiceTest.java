package com.grash.service;

import com.grash.model.UserSettings;
import com.grash.repository.UserSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSettingsServiceTest {

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @InjectMocks
    private UserSettingsService userSettingsService;

    private UserSettings userSettings;

    @BeforeEach
    void setUp() {
        userSettings = new UserSettings();
        userSettings.setId(1L);
    }

    @Test
    void create() {
        when(userSettingsRepository.save(any(UserSettings.class))).thenReturn(userSettings);

        UserSettings result = userSettingsService.create(userSettings);

        assertNotNull(result);
        assertEquals(userSettings.getId(), result.getId());
        verify(userSettingsRepository).save(userSettings);
    }

    @Test
    void update() {
        when(userSettingsRepository.save(any(UserSettings.class))).thenReturn(userSettings);

        UserSettings result = userSettingsService.update(userSettings);

        assertNotNull(result);
        assertEquals(userSettings.getId(), result.getId());
        verify(userSettingsRepository).save(userSettings);
    }

    @Test
    void getAll() {
        when(userSettingsRepository.findAll()).thenReturn(Collections.singletonList(userSettings));

        assertEquals(1, userSettingsService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(userSettingsRepository).deleteById(1L);
        userSettingsService.delete(1L);
        verify(userSettingsRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(userSettingsRepository.findById(1L)).thenReturn(Optional.of(userSettings));

        assertTrue(userSettingsService.findById(1L).isPresent());
    }
}
