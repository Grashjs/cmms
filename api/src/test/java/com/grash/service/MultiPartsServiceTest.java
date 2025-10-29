package com.grash.service;

import com.grash.dto.MultiPartsPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.MultiPartsMapper;
import com.grash.model.Company;
import com.grash.model.MultiParts;
import com.grash.repository.MultiPartsRepository;
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
class MultiPartsServiceTest {

    @Mock
    private MultiPartsRepository multiPartsRepository;

    @Mock
    private MultiPartsMapper multiPartsMapper;

    @Mock
    private EntityManager em;

    @InjectMocks
    private MultiPartsService multiPartsService;

    private MultiParts multiParts;
    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);

        multiParts = new MultiParts();
        multiParts.setId(1L);
        multiParts.setCompany(company);
    }

    @Test
    void create() {
        when(multiPartsRepository.saveAndFlush(any(MultiParts.class))).thenReturn(multiParts);

        MultiParts result = multiPartsService.create(multiParts);

        assertNotNull(result);
        assertEquals(multiParts.getId(), result.getId());
        verify(multiPartsRepository).saveAndFlush(multiParts);
        verify(em).refresh(multiParts);
    }

    @Test
    void update_whenExists() {
        MultiPartsPatchDTO patchDTO = new MultiPartsPatchDTO();
        when(multiPartsRepository.existsById(1L)).thenReturn(true);
        when(multiPartsRepository.findById(1L)).thenReturn(Optional.of(multiParts));
        when(multiPartsRepository.saveAndFlush(any(MultiParts.class))).thenReturn(multiParts);
        when(multiPartsMapper.updateMultiParts(any(MultiParts.class), any(MultiPartsPatchDTO.class))).thenReturn(multiParts);

        MultiParts result = multiPartsService.update(1L, patchDTO);

        assertNotNull(result);
        assertEquals(multiParts.getId(), result.getId());
        verify(multiPartsRepository).saveAndFlush(multiParts);
        verify(em).refresh(multiParts);
    }

    @Test
    void update_whenNotExists_shouldThrowException() {
        MultiPartsPatchDTO patchDTO = new MultiPartsPatchDTO();
        when(multiPartsRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class, () -> multiPartsService.update(1L, patchDTO));

        assertEquals("Not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void getAll() {
        when(multiPartsRepository.findAll()).thenReturn(Collections.singletonList(multiParts));

        assertEquals(1, multiPartsService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(multiPartsRepository).deleteById(1L);
        multiPartsService.delete(1L);
        verify(multiPartsRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(multiPartsRepository.findById(1L)).thenReturn(Optional.of(multiParts));

        assertTrue(multiPartsService.findById(1L).isPresent());
    }

    @Test
    void findByCompany() {
        when(multiPartsRepository.findByCompany_Id(1L)).thenReturn(Collections.singletonList(multiParts));

        assertEquals(1, multiPartsService.findByCompany(1L).size());
    }
}
