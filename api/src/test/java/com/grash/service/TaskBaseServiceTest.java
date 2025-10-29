package com.grash.service;

import com.grash.dto.TaskBaseDTO;
import com.grash.dto.TaskBasePatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.TaskBaseMapper;
import com.grash.model.Company;
import com.grash.model.TaskBase;
import com.grash.repository.TaskBaseRepository;
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
class TaskBaseServiceTest {

    @Mock
    private TaskBaseRepository taskBaseRepository;

    @Mock
    private TaskBaseMapper taskBaseMapper;

    @Mock
    private EntityManager em;

    @InjectMocks
    private TaskBaseService taskBaseService;

    private TaskBase taskBase;
    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);

        taskBase = new TaskBase();
        taskBase.setId(1L);
    }

    @Test
    void create() {
        when(taskBaseRepository.saveAndFlush(any(TaskBase.class))).thenReturn(taskBase);

        TaskBase result = taskBaseService.create(taskBase);

        assertNotNull(result);
        assertEquals(taskBase.getId(), result.getId());
        verify(taskBaseRepository).saveAndFlush(taskBase);
        verify(em).refresh(taskBase);
    }

    @Test
    void createFromTaskBaseDTO() {
        TaskBaseDTO dto = new TaskBaseDTO();
        dto.setLabel("Test Task");

        when(taskBaseRepository.saveAndFlush(any(TaskBase.class))).thenReturn(taskBase);

        TaskBase result = taskBaseService.createFromTaskBaseDTO(dto, company);

        assertNotNull(result);
        assertEquals(taskBase.getId(), result.getId());
    }

    @Test
    void update_whenExists() {
        TaskBasePatchDTO patchDTO = new TaskBasePatchDTO();
        when(taskBaseRepository.existsById(1L)).thenReturn(true);
        when(taskBaseRepository.findById(1L)).thenReturn(Optional.of(taskBase));
        when(taskBaseRepository.save(any(TaskBase.class))).thenReturn(taskBase);
        when(taskBaseMapper.updateTaskBase(any(TaskBase.class), any(TaskBasePatchDTO.class))).thenReturn(taskBase);

        TaskBase result = taskBaseService.update(1L, patchDTO);

        assertNotNull(result);
        assertEquals(taskBase.getId(), result.getId());
        verify(taskBaseRepository).save(taskBase);
    }

    @Test
    void update_whenNotExists_shouldThrowException() {
        TaskBasePatchDTO patchDTO = new TaskBasePatchDTO();
        when(taskBaseRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class, () -> taskBaseService.update(1L, patchDTO));

        assertEquals("Not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void getAll() {
        when(taskBaseRepository.findAll()).thenReturn(Collections.singletonList(taskBase));

        assertEquals(1, taskBaseService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(taskBaseRepository).deleteById(1L);
        taskBaseService.delete(1L);
        verify(taskBaseRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(taskBaseRepository.findById(1L)).thenReturn(Optional.of(taskBase));

        assertTrue(taskBaseService.findById(1L).isPresent());
    }
}
