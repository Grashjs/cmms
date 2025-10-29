package com.grash.service;

import com.grash.dto.SubscriptionPlanPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.SubscriptionPlanMapper;
import com.grash.model.SubscriptionPlan;
import com.grash.repository.SubscriptionPlanRepository;
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
class SubscriptionPlanServiceTest {

    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Mock
    private SubscriptionPlanMapper subscriptionPlanMapper;

    @InjectMocks
    private SubscriptionPlanService subscriptionPlanService;

    private SubscriptionPlan subscriptionPlan;

    @BeforeEach
    void setUp() {
        subscriptionPlan = new SubscriptionPlan();
        subscriptionPlan.setId(1L);
        subscriptionPlan.setCode("TEST");
    }

    @Test
    void create() {
        when(subscriptionPlanRepository.save(any(SubscriptionPlan.class))).thenReturn(subscriptionPlan);

        SubscriptionPlan result = subscriptionPlanService.create(subscriptionPlan);

        assertNotNull(result);
        assertEquals(subscriptionPlan.getId(), result.getId());
        verify(subscriptionPlanRepository).save(subscriptionPlan);
    }

    @Test
    void update_whenExists() {
        SubscriptionPlanPatchDTO patchDTO = new SubscriptionPlanPatchDTO();
        when(subscriptionPlanRepository.existsById(1L)).thenReturn(true);
        when(subscriptionPlanRepository.findById(1L)).thenReturn(Optional.of(subscriptionPlan));
        when(subscriptionPlanRepository.save(any(SubscriptionPlan.class))).thenReturn(subscriptionPlan);
        when(subscriptionPlanMapper.updateSubscriptionPlan(any(SubscriptionPlan.class), any(SubscriptionPlanPatchDTO.class))).thenReturn(subscriptionPlan);

        SubscriptionPlan result = subscriptionPlanService.update(1L, patchDTO);

        assertNotNull(result);
        assertEquals(subscriptionPlan.getId(), result.getId());
        verify(subscriptionPlanRepository).save(subscriptionPlan);
    }

    @Test
    void update_whenNotExists_shouldThrowException() {
        SubscriptionPlanPatchDTO patchDTO = new SubscriptionPlanPatchDTO();
        when(subscriptionPlanRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class, () -> subscriptionPlanService.update(1L, patchDTO));

        assertEquals("Not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void getAll() {
        when(subscriptionPlanRepository.findAll()).thenReturn(Collections.singletonList(subscriptionPlan));

        assertEquals(1, subscriptionPlanService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(subscriptionPlanRepository).deleteById(1L);
        subscriptionPlanService.delete(1L);
        verify(subscriptionPlanRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(subscriptionPlanRepository.findById(1L)).thenReturn(Optional.of(subscriptionPlan));

        assertTrue(subscriptionPlanService.findById(1L).isPresent());
    }

    @Test
    void findByCode() {
        when(subscriptionPlanRepository.findByCode("TEST")).thenReturn(Optional.of(subscriptionPlan));

        assertTrue(subscriptionPlanService.findByCode("TEST").isPresent());
    }

    @Test
    void existByCode() {
        when(subscriptionPlanRepository.existsByCode("TEST")).thenReturn(true);

        assertTrue(subscriptionPlanService.existByCode("TEST"));
    }
}
