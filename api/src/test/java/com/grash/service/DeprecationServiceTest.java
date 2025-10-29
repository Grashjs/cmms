package com.grash.service;

import com.grash.dto.DeprecationPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.DeprecationMapper;
import com.grash.model.Deprecation;
import com.grash.repository.DeprecationRepository;
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
class DeprecationServiceTest {

    @Mock
    private DeprecationRepository deprecationRepository;

    @Mock
    private DeprecationMapper deprecationMapper;

    @InjectMocks
    private DeprecationService deprecationService;

    private Deprecation deprecation;

    @BeforeEach
    void setUp() {
        deprecation = new Deprecation();
        deprecation.setId(1L);
    }

    @Test
    void create() {
        when(deprecationRepository.save(any(Deprecation.class))).thenReturn(deprecation);

        Deprecation result = deprecationService.create(deprecation);

        assertNotNull(result);
        assertEquals(deprecation.getId(), result.getId());
        verify(deprecationRepository).save(deprecation);
    }

    @Test
    void update_whenExists() {
        DeprecationPatchDTO patchDTO = new DeprecationPatchDTO();
        when(deprecationRepository.existsById(1L)).thenReturn(true);
        when(deprecationRepository.findById(1L)).thenReturn(Optional.of(deprecation));
        when(deprecationRepository.save(any(Deprecation.class))).thenReturn(deprecation);
        when(deprecationMapper.updateDeprecation(any(Deprecation.class), any(DeprecationPatchDTO.class))).thenReturn(deprecation);

        Deprecation result = deprecationService.update(1L, patchDTO);

        assertNotNull(result);
        assertEquals(deprecation.getId(), result.getId());
        verify(deprecationRepository).save(deprecation);
    }

    @Test
    void update_whenNotExists_shouldThrowException() {
        DeprecationPatchDTO patchDTO = new DeprecationPatchDTO();
        when(deprecationRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class, () -> deprecationService.update(1L, patchDTO));

        assertEquals("Not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void getAll() {
        when(deprecationRepository.findAll()).thenReturn(Collections.singletonList(deprecation));

        assertEquals(1, deprecationService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(deprecationRepository).deleteById(1L);
        deprecationService.delete(1L);
        verify(deprecationRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(deprecationRepository.findById(1L)).thenReturn(Optional.of(deprecation));

        assertTrue(deprecationService.findById(1L).isPresent());
    }
}
