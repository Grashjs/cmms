package com.grash.service;

import com.grash.dto.FloorPlanPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.FloorPlanMapper;
import com.grash.model.FloorPlan;
import com.grash.model.Location;
import com.grash.repository.FloorPlanRepository;
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
class FloorPlanServiceTest {

    @Mock
    private FloorPlanRepository floorPlanRepository;

    @Mock
    private FloorPlanMapper floorPlanMapper;

    @Mock
    private EntityManager em;

    @InjectMocks
    private FloorPlanService floorPlanService;

    private FloorPlan floorPlan;
    private Location location;

    @BeforeEach
    void setUp() {
        location = new Location();
        location.setId(1L);

        floorPlan = new FloorPlan();
        floorPlan.setId(1L);
        floorPlan.setLocation(location);
    }

    @Test
    void create() {
        when(floorPlanRepository.saveAndFlush(any(FloorPlan.class))).thenReturn(floorPlan);

        FloorPlan result = floorPlanService.create(floorPlan);

        assertNotNull(result);
        assertEquals(floorPlan.getId(), result.getId());
        verify(floorPlanRepository).saveAndFlush(floorPlan);
        verify(em).refresh(floorPlan);
    }

    @Test
    void update_whenExists() {
        FloorPlanPatchDTO patchDTO = new FloorPlanPatchDTO();
        when(floorPlanRepository.existsById(1L)).thenReturn(true);
        when(floorPlanRepository.findById(1L)).thenReturn(Optional.of(floorPlan));
        when(floorPlanRepository.saveAndFlush(any(FloorPlan.class))).thenReturn(floorPlan);
        when(floorPlanMapper.updateFloorPlan(any(FloorPlan.class), any(FloorPlanPatchDTO.class))).thenReturn(floorPlan);

        FloorPlan result = floorPlanService.update(1L, patchDTO);

        assertNotNull(result);
        assertEquals(floorPlan.getId(), result.getId());
        verify(floorPlanRepository).saveAndFlush(floorPlan);
        verify(em).refresh(floorPlan);
    }

    @Test
    void update_whenNotExists_shouldThrowException() {
        FloorPlanPatchDTO patchDTO = new FloorPlanPatchDTO();
        when(floorPlanRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class, () -> floorPlanService.update(1L, patchDTO));

        assertEquals("Not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void getAll() {
        when(floorPlanRepository.findAll()).thenReturn(Collections.singletonList(floorPlan));

        assertEquals(1, floorPlanService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(floorPlanRepository).deleteById(1L);
        floorPlanService.delete(1L);
        verify(floorPlanRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(floorPlanRepository.findById(1L)).thenReturn(Optional.of(floorPlan));

        assertTrue(floorPlanService.findById(1L).isPresent());
    }

    @Test
    void findByLocation() {
        when(floorPlanRepository.findByLocation_Id(1L)).thenReturn(Collections.singletonList(floorPlan));

        assertEquals(1, floorPlanService.findByLocation(1L).size());
    }
}
