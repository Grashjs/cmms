package com.grash.service;

import com.grash.dto.CurrencyPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.CurrencyMapper;
import com.grash.model.Currency;
import com.grash.repository.CurrencyRepository;
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
class CurrencyServiceTest {

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private CurrencyMapper currencyMapper;

    @InjectMocks
    private CurrencyService currencyService;

    private Currency currency;

    @BeforeEach
    void setUp() {
        currency = new Currency();
        currency.setId(1L);
        currency.setCode("USD");
    }

    @Test
    void create() {
        when(currencyRepository.save(any(Currency.class))).thenReturn(currency);

        Currency result = currencyService.create(currency);

        assertNotNull(result);
        assertEquals(currency.getId(), result.getId());
        verify(currencyRepository).save(currency);
    }

    @Test
    void update_whenExists() {
        CurrencyPatchDTO patchDTO = new CurrencyPatchDTO();
        when(currencyRepository.existsById(1L)).thenReturn(true);
        when(currencyRepository.findById(1L)).thenReturn(Optional.of(currency));
        when(currencyRepository.save(any(Currency.class))).thenReturn(currency);
        when(currencyMapper.updateCurrency(any(Currency.class), any(CurrencyPatchDTO.class))).thenReturn(currency);

        Currency result = currencyService.update(1L, patchDTO);

        assertNotNull(result);
        assertEquals(currency.getId(), result.getId());
        verify(currencyRepository).save(currency);
    }

    @Test
    void update_whenNotExists_shouldThrowException() {
        CurrencyPatchDTO patchDTO = new CurrencyPatchDTO();
        when(currencyRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class, () -> currencyService.update(1L, patchDTO));

        assertEquals("Not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void getAll() {
        when(currencyRepository.findAll()).thenReturn(Collections.singletonList(currency));

        assertEquals(1, currencyService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(currencyRepository).deleteById(1L);
        currencyService.delete(1L);
        verify(currencyRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(currencyRepository.findById(1L)).thenReturn(Optional.of(currency));

        assertTrue(currencyService.findById(1L).isPresent());
    }

    @Test
    void findByCode() {
        when(currencyRepository.findByCode("USD")).thenReturn(Optional.of(currency));

        assertTrue(currencyService.findByCode("USD").isPresent());
    }
}
