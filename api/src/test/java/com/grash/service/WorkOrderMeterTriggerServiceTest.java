package com.grash.service;

import com.grash.dto.WorkOrderMeterTriggerPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.WorkOrderMeterTriggerMapper;
import com.grash.model.Meter;
import com.grash.model.WorkOrderMeterTrigger;
import com.grash.repository.WorkOrderMeterTriggerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import javax.persistence.EntityManager;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkOrderMeterTriggerServiceTest {

    @Mock
    private WorkOrderMeterTriggerRepository workOrderMeterTriggerRepository;

    @Mock
    private WorkOrderMeterTriggerMapper workOrderMeterTriggerMapper;

    @Mock
    private EntityManager em;

    @InjectMocks
    private WorkOrderMeterTriggerService workOrderMeterTriggerService;

    private WorkOrderMeterTrigger workOrderMeterTrigger;
    private Meter meter;

    @BeforeEach
    void setUp() {
        meter = new Meter();
        meter.setId(1L);

        workOrderMeterTrigger = new WorkOrderMeterTrigger();
        workOrderMeterTrigger.setId(1L);
        workOrderMeterTrigger.setMeter(meter);
    }

    @Test
    void create() {
        when(workOrderMeterTriggerRepository.saveAndFlush(any(WorkOrderMeterTrigger.class))).thenReturn(workOrderMeterTrigger);

        WorkOrderMeterTrigger result = workOrderMeterTriggerService.create(workOrderMeterTrigger);

        assertNotNull(result);
        assertEquals(workOrderMeterTrigger.getId(), result.getId());
        verify(workOrderMeterTriggerRepository).saveAndFlush(workOrderMeterTrigger);
        verify(em).refresh(workOrderMeterTrigger);
    }

    @Test
    void update_whenExists() {
        WorkOrderMeterTriggerPatchDTO patchDTO = new WorkOrderMeterTriggerPatchDTO();
        when(workOrderMeterTriggerRepository.existsById(1L)).thenReturn(true);
        when(workOrderMeterTriggerRepository.findById(1L)).thenReturn(Optional.of(workOrderMeterTrigger));
        when(workOrderMeterTriggerRepository.save(any(WorkOrderMeterTrigger.class))).thenReturn(workOrderMeterTrigger);
        when(workOrderMeterTriggerMapper.updateWorkOrderMeterTrigger(any(WorkOrderMeterTrigger.class), any(WorkOrderMeterTriggerPatchDTO.class))).thenReturn(workOrderMeterTrigger);

        WorkOrderMeterTrigger result = workOrderMeterTriggerService.update(1L, patchDTO);

        assertNotNull(result);
        assertEquals(workOrderMeterTrigger.getId(), result.getId());
        verify(workOrderMeterTriggerRepository).save(workOrderMeterTrigger);
    }

    @Test
    void update_whenNotExists_shouldThrowException() {
        WorkOrderMeterTriggerPatchDTO patchDTO = new WorkOrderMeterTriggerPatchDTO();
        when(workOrderMeterTriggerRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class, () -> workOrderMeterTriggerService.update(1L, patchDTO));

        assertEquals("Not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void getAll() {
        when(workOrderMeterTriggerRepository.findAll()).thenReturn(Collections.singletonList(workOrderMeterTrigger));

        assertEquals(1, workOrderMeterTriggerService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(workOrderMeterTriggerRepository).deleteById(1L);
        workOrderMeterTriggerService.delete(1L);
        verify(workOrderMeterTriggerRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(workOrderMeterTriggerRepository.findById(1L)).thenReturn(Optional.of(workOrderMeterTrigger));

        assertTrue(workOrderMeterTriggerService.findById(1L).isPresent());
    }

    @Test
    void findByMeter() {
        when(workOrderMeterTriggerRepository.findByMeter_Id(1L)).thenReturn(Collections.singletonList(workOrderMeterTrigger));

        assertEquals(1, workOrderMeterTriggerService.findByMeter(1L).size());
    }
}
