package com.grash.service;

import com.grash.dto.TaskOptionPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.TaskOptionMapper;
import com.grash.model.Company;
import com.grash.model.TaskOption;
import com.grash.repository.TaskOptionRepository;
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
class TaskOptionServiceTest {

    @Mock
    private TaskOptionRepository taskOptionRepository;

    @Mock
    private TaskOptionMapper taskOptionMapper;

    @InjectMocks
    private TaskOptionService taskOptionService;

    private TaskOption taskOption;
    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);

        taskOption = new TaskOption();
        taskOption.setId(1L);
        taskOption.setCompany(company);
    }

    @Test
    void create() {
        when(taskOptionRepository.save(any(TaskOption.class))).thenReturn(taskOption);

        TaskOption result = taskOptionService.create(taskOption);

        assertNotNull(result);
        assertEquals(taskOption.getId(), result.getId());
        verify(taskOptionRepository).save(taskOption);
    }

    @Test
    void update_whenExists() {
        TaskOptionPatchDTO patchDTO = new TaskOptionPatchDTO();
        when(taskOptionRepository.existsById(1L)).thenReturn(true);
        when(taskOptionRepository.findById(1L)).thenReturn(Optional.of(taskOption));
        when(taskOptionRepository.save(any(TaskOption.class))).thenReturn(taskOption);
        when(taskOptionMapper.updateTaskOption(any(TaskOption.class), any(TaskOptionPatchDTO.class))).thenReturn(taskOption);

        TaskOption result = taskOptionService.update(1L, patchDTO);

        assertNotNull(result);
        assertEquals(taskOption.getId(), result.getId());
        verify(taskOptionRepository).save(taskOption);
    }

    @Test
    void update_whenNotExists_shouldThrowException() {
        TaskOptionPatchDTO patchDTO = new TaskOptionPatchDTO();
        when(taskOptionRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class, () -> taskOptionService.update(1L, patchDTO));

        assertEquals("Not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void getAll() {
        when(taskOptionRepository.findAll()).thenReturn(Collections.singletonList(taskOption));

        assertEquals(1, taskOptionService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(taskOptionRepository).deleteById(1L);
        taskOptionService.delete(1L);
        verify(taskOptionRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(taskOptionRepository.findById(1L)).thenReturn(Optional.of(taskOption));

        assertTrue(taskOptionService.findById(1L).isPresent());
    }
}
