package com.grash.service;

import com.grash.dto.AdditionalCostPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.AdditionalCostMapper;
import com.grash.model.AdditionalCost;
import com.grash.repository.AdditionalCostRepository;
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
class AdditionalCostServiceTest {

    @Mock
    private AdditionalCostRepository additionalCostRepository;

    @Mock
    private EntityManager em;

    @Mock
    private AdditionalCostMapper additionalCostMapper;

    @InjectMocks
    private AdditionalCostService additionalCostService;

    private AdditionalCost additionalCost;

    @BeforeEach
    void setUp() {
        additionalCost = new AdditionalCost();
        additionalCost.setId(1L);
    }

    @Test
    void create() {
        when(additionalCostRepository.saveAndFlush(any(AdditionalCost.class))).thenReturn(additionalCost);

        AdditionalCost result = additionalCostService.create(additionalCost);

        assertNotNull(result);
        assertEquals(additionalCost.getId(), result.getId());
        verify(additionalCostRepository).saveAndFlush(additionalCost);
        verify(em).refresh(additionalCost);
    }

    @Test
    void update_whenExists() {
        AdditionalCostPatchDTO patchDTO = new AdditionalCostPatchDTO();
        when(additionalCostRepository.existsById(1L)).thenReturn(true);
        when(additionalCostRepository.findById(1L)).thenReturn(Optional.of(additionalCost));
        when(additionalCostRepository.saveAndFlush(any(AdditionalCost.class))).thenReturn(additionalCost);
        when(additionalCostMapper.updateAdditionalCost(any(AdditionalCost.class), any(AdditionalCostPatchDTO.class))).thenReturn(additionalCost);

        AdditionalCost result = additionalCostService.update(1L, patchDTO);

        assertNotNull(result);
        assertEquals(additionalCost.getId(), result.getId());
        verify(additionalCostRepository).saveAndFlush(additionalCost);
        verify(em).refresh(additionalCost);
    }

    @Test
    void update_whenNotExists_shouldThrowException() {
        AdditionalCostPatchDTO patchDTO = new AdditionalCostPatchDTO();
        when(additionalCostRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class, () -> additionalCostService.update(1L, patchDTO));

        assertEquals("Not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void getAll() {
        when(additionalCostRepository.findAll()).thenReturn(Collections.singletonList(additionalCost));

        assertEquals(1, additionalCostService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(additionalCostRepository).deleteById(1L);
        additionalCostService.delete(1L);
        verify(additionalCostRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(additionalCostRepository.findById(1L)).thenReturn(Optional.of(additionalCost));

        assertTrue(additionalCostService.findById(1L).isPresent());
    }

    @Test
    void findByWorkOrder() {
        when(additionalCostRepository.findByWorkOrder_Id(1L)).thenReturn(Collections.singletonList(additionalCost));

        assertEquals(1, additionalCostService.findByWorkOrder(1L).size());
    }
}
