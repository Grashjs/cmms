package com.grash.service;

import com.grash.dto.LaborPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.LaborMapper;
import com.grash.model.Labor;
import com.grash.model.WorkOrder;
import com.grash.model.enums.TimeStatus;
import com.grash.repository.LaborRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import javax.persistence.EntityManager;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LaborServiceTest {

    @Mock
    private LaborRepository laborRepository;

    @Mock
    private LaborMapper laborMapper;

    @Mock
    private EntityManager em;

    @InjectMocks
    private LaborService laborService;

    private Labor labor;
    private WorkOrder workOrder;

    @BeforeEach
    void setUp() {
        workOrder = new WorkOrder();
        workOrder.setId(1L);

        labor = new Labor();
        labor.setId(1L);
        labor.setWorkOrder(workOrder);
    }

    @Test
    void create() {
        when(laborRepository.saveAndFlush(any(Labor.class))).thenReturn(labor);

        Labor result = laborService.create(labor);

        assertNotNull(result);
        assertEquals(labor.getId(), result.getId());
        verify(laborRepository).saveAndFlush(labor);
        verify(em).refresh(labor);
    }

    @Test
    void update_whenExists() {
        LaborPatchDTO patchDTO = new LaborPatchDTO();
        when(laborRepository.existsById(1L)).thenReturn(true);
        when(laborRepository.findById(1L)).thenReturn(Optional.of(labor));
        when(laborRepository.saveAndFlush(any(Labor.class))).thenReturn(labor);
        when(laborMapper.updateLabor(any(Labor.class), any(LaborPatchDTO.class))).thenReturn(labor);

        Labor result = laborService.update(1L, patchDTO);

        assertNotNull(result);
        assertEquals(labor.getId(), result.getId());
        verify(laborRepository).saveAndFlush(labor);
        verify(em).refresh(labor);
    }

    @Test
    void update_whenNotExists_shouldThrowException() {
        LaborPatchDTO patchDTO = new LaborPatchDTO();
        when(laborRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class, () -> laborService.update(1L, patchDTO));

        assertEquals("Not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void save() {
        when(laborRepository.save(any(Labor.class))).thenReturn(labor);

        Labor result = laborService.save(labor);

        assertNotNull(result);
        assertEquals(labor.getId(), result.getId());
        verify(laborRepository).save(labor);
    }

    @Test
    void getAll() {
        when(laborRepository.findAll()).thenReturn(Collections.singletonList(labor));

        assertEquals(1, laborService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(laborRepository).deleteById(1L);
        laborService.delete(1L);
        verify(laborRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(laborRepository.findById(1L)).thenReturn(Optional.of(labor));

        assertTrue(laborService.findById(1L).isPresent());
    }

    @Test
    void findByWorkOrder() {
        when(laborRepository.findByWorkOrder_Id(1L)).thenReturn(Collections.singletonList(labor));

        assertEquals(1, laborService.findByWorkOrder(1L).size());
    }

    @Test
    void stop() {
        labor.setStartedAt(new Date(System.currentTimeMillis() - 10000));
        labor.setDuration(0L);
        when(laborRepository.save(any(Labor.class))).thenReturn(labor);

        Labor result = laborService.stop(labor);

        assertEquals(TimeStatus.STOPPED, result.getStatus());
        assertTrue(result.getDuration() > 0);
    }
}
