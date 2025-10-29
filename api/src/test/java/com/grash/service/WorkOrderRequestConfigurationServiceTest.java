package com.grash.service;

import com.grash.model.WorkOrderRequestConfiguration;
import com.grash.repository.WorkOrderRequestConfigurationRepository;
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
class WorkOrderRequestConfigurationServiceTest {

    @Mock
    private WorkOrderRequestConfigurationRepository workOrderRequestConfigurationRepository;

    @InjectMocks
    private WorkOrderRequestConfigurationService workOrderRequestConfigurationService;

    private WorkOrderRequestConfiguration workOrderRequestConfiguration;

    @BeforeEach
    void setUp() {
        workOrderRequestConfiguration = new WorkOrderRequestConfiguration();
        workOrderRequestConfiguration.setId(1L);
    }

    @Test
    void create() {
        when(workOrderRequestConfigurationRepository.save(any(WorkOrderRequestConfiguration.class))).thenReturn(workOrderRequestConfiguration);

        WorkOrderRequestConfiguration result = workOrderRequestConfigurationService.create(workOrderRequestConfiguration);

        assertNotNull(result);
        assertEquals(workOrderRequestConfiguration.getId(), result.getId());
        verify(workOrderRequestConfigurationRepository).save(workOrderRequestConfiguration);
    }

    @Test
    void update() {
        when(workOrderRequestConfigurationRepository.save(any(WorkOrderRequestConfiguration.class))).thenReturn(workOrderRequestConfiguration);

        WorkOrderRequestConfiguration result = workOrderRequestConfigurationService.update(workOrderRequestConfiguration);

        assertNotNull(result);
        assertEquals(workOrderRequestConfiguration.getId(), result.getId());
        verify(workOrderRequestConfigurationRepository).save(workOrderRequestConfiguration);
    }

    @Test
    void getAll() {
        when(workOrderRequestConfigurationRepository.findAll()).thenReturn(Collections.singletonList(workOrderRequestConfiguration));

        assertEquals(1, workOrderRequestConfigurationService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(workOrderRequestConfigurationRepository).deleteById(1L);
        workOrderRequestConfigurationService.delete(1L);
        verify(workOrderRequestConfigurationRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(workOrderRequestConfigurationRepository.findById(1L)).thenReturn(Optional.of(workOrderRequestConfiguration));

        assertTrue(workOrderRequestConfigurationService.findById(1L).isPresent());
    }
}
