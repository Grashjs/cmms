package com.grash.service;

import com.grash.model.WorkOrderConfiguration;
import com.grash.repository.WorkOrderConfigurationRepository;
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
class WorkOrderConfigurationServiceTest {

    @Mock
    private WorkOrderConfigurationRepository workOrderConfigurationRepository;

    @InjectMocks
    private WorkOrderConfigurationService workOrderConfigurationService;

    private WorkOrderConfiguration workOrderConfiguration;

    @BeforeEach
    void setUp() {
        workOrderConfiguration = new WorkOrderConfiguration();
        workOrderConfiguration.setId(1L);
    }

    @Test
    void create() {
        when(workOrderConfigurationRepository.save(any(WorkOrderConfiguration.class))).thenReturn(workOrderConfiguration);

        WorkOrderConfiguration result = workOrderConfigurationService.create(workOrderConfiguration);

        assertNotNull(result);
        assertEquals(workOrderConfiguration.getId(), result.getId());
        verify(workOrderConfigurationRepository).save(workOrderConfiguration);
    }

    @Test
    void update() {
        when(workOrderConfigurationRepository.save(any(WorkOrderConfiguration.class))).thenReturn(workOrderConfiguration);

        WorkOrderConfiguration result = workOrderConfigurationService.update(workOrderConfiguration);

        assertNotNull(result);
        assertEquals(workOrderConfiguration.getId(), result.getId());
        verify(workOrderConfigurationRepository).save(workOrderConfiguration);
    }

    @Test
    void getAll() {
        when(workOrderConfigurationRepository.findAll()).thenReturn(Collections.singletonList(workOrderConfiguration));

        assertEquals(1, workOrderConfigurationService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(workOrderConfigurationRepository).deleteById(1L);
        workOrderConfigurationService.delete(1L);
        verify(workOrderConfigurationRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(workOrderConfigurationRepository.findById(1L)).thenReturn(Optional.of(workOrderConfiguration));

        assertTrue(workOrderConfigurationService.findById(1L).isPresent());
    }
}
