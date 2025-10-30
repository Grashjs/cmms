package com.grash.service;

import com.grash.dto.ReadingPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.ReadingMapper;
import com.grash.model.Company;
import com.grash.model.Meter;
import com.grash.model.Reading;
import com.grash.repository.ReadingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
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
class ReadingServiceTest {

    @Mock
    private ReadingRepository readingRepository;

    @Mock
    private ReadingMapper readingMapper;

    @InjectMocks
    private ReadingService readingService;

    private Reading reading;
    private Company company;
    private Meter meter;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);

        meter = new Meter();
        meter.setId(1L);

        reading = new Reading();
        reading.setId(1L);
        meter.setCompany(company);
        reading.setMeter(meter);
    }

    @Nested
    class Create {
        @Test
        void create() {
            when(readingRepository.save(any(Reading.class))).thenReturn(reading);

            Reading result = readingService.create(reading);

            assertNotNull(result);
            assertEquals(reading.getId(), result.getId());
            verify(readingRepository).save(reading);
        }
    }

    @Nested
    class Update {
        @Test
        void update_whenExists() {
            ReadingPatchDTO patchDTO = new ReadingPatchDTO();
            when(readingRepository.existsById(1L)).thenReturn(true);
            when(readingRepository.findById(1L)).thenReturn(Optional.of(reading));
            when(readingRepository.save(any(Reading.class))).thenReturn(reading);
            when(readingMapper.updateReading(any(Reading.class), any(ReadingPatchDTO.class))).thenReturn(reading);

            Reading result = readingService.update(1L, patchDTO);

            assertNotNull(result);
            assertEquals(reading.getId(), result.getId());
            verify(readingRepository).save(reading);
        }

        @Test
        void update_whenNotExists_shouldThrowException() {
            ReadingPatchDTO patchDTO = new ReadingPatchDTO();
            when(readingRepository.existsById(1L)).thenReturn(false);

            CustomException exception = assertThrows(CustomException.class, () -> readingService.update(1L, patchDTO));

            assertEquals("Not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }
    }

    @Nested
    class GetAll {
        @Test
        void getAll() {
            when(readingRepository.findAll()).thenReturn(Collections.singletonList(reading));

            assertEquals(1, readingService.getAll().size());
        }
    }

    @Nested
    class Delete {
        @Test
        void delete() {
            doNothing().when(readingRepository).deleteById(1L);
            readingService.delete(1L);
            verify(readingRepository).deleteById(1L);
        }
    }

    @Nested
    class FindById {
        @Test
        void findById_whenFound_shouldReturnOptionalOfReading() {
            when(readingRepository.findById(1L)).thenReturn(Optional.of(reading));

            assertTrue(readingService.findById(1L).isPresent());
        }

        @Test
        void findById_whenNotFound_shouldReturnEmptyOptional() {
            when(readingRepository.findById(1L)).thenReturn(Optional.empty());

            assertFalse(readingService.findById(1L).isPresent());
        }
    }

    @Nested
    class FindByCompany {
        @Test
        void findByCompany() {
            when(readingRepository.findByCompany_Id(1L)).thenReturn(Collections.singletonList(reading));

            assertEquals(1, readingService.findByCompany(1L).size());
        }
    }

    @Nested
    class FindByMeter {
        @Test
        void findByMeter() {
            when(readingRepository.findByMeter_Id(1L)).thenReturn(Collections.singletonList(reading));

            assertEquals(1, readingService.findByMeter(1L).size());
        }
    }
}
