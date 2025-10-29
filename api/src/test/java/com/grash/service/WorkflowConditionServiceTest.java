package com.grash.service;

import com.grash.dto.WorkflowConditionPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.WorkflowConditionMapper;
import com.grash.model.WorkflowCondition;
import com.grash.repository.WorkflowConditionRepository;
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
class WorkflowConditionServiceTest {

    @Mock
    private WorkflowConditionRepository workflowConditionRepository;

    @Mock
    private WorkflowConditionMapper workflowConditionMapper;

    @InjectMocks
    private WorkflowConditionService workflowConditionService;

    private WorkflowCondition workflowCondition;

    @BeforeEach
    void setUp() {
        workflowCondition = new WorkflowCondition();
        workflowCondition.setId(1L);
    }

    @Test
    void create() {
        when(workflowConditionRepository.save(any(WorkflowCondition.class))).thenReturn(workflowCondition);

        WorkflowCondition result = workflowConditionService.create(workflowCondition);

        assertNotNull(result);
        assertEquals(workflowCondition.getId(), result.getId());
        verify(workflowConditionRepository).save(workflowCondition);
    }

    @Test
    void update_whenExists() {
        WorkflowConditionPatchDTO patchDTO = new WorkflowConditionPatchDTO();
        when(workflowConditionRepository.existsById(1L)).thenReturn(true);
        when(workflowConditionRepository.findById(1L)).thenReturn(Optional.of(workflowCondition));
        when(workflowConditionRepository.save(any(WorkflowCondition.class))).thenReturn(workflowCondition);
        when(workflowConditionMapper.updateWorkflowCondition(any(WorkflowCondition.class), any(WorkflowConditionPatchDTO.class))).thenReturn(workflowCondition);

        WorkflowCondition result = workflowConditionService.update(1L, patchDTO);

        assertNotNull(result);
        assertEquals(workflowCondition.getId(), result.getId());
        verify(workflowConditionRepository).save(workflowCondition);
    }

    @Test
    void update_whenNotExists_shouldThrowException() {
        WorkflowConditionPatchDTO patchDTO = new WorkflowConditionPatchDTO();
        when(workflowConditionRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class, () -> workflowConditionService.update(1L, patchDTO));

        assertEquals("Not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void getAll() {
        when(workflowConditionRepository.findAll()).thenReturn(Collections.singletonList(workflowCondition));

        assertEquals(1, workflowConditionService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(workflowConditionRepository).deleteById(1L);
        workflowConditionService.delete(1L);
        verify(workflowConditionRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(workflowConditionRepository.findById(1L)).thenReturn(Optional.of(workflowCondition));

        assertTrue(workflowConditionService.findById(1L).isPresent());
    }

    @Test
    void saveAll() {
        when(workflowConditionRepository.saveAll(any())).thenReturn(Collections.singletonList(workflowCondition));

        assertEquals(1, workflowConditionService.saveAll(Collections.singletonList(workflowCondition)).size());
    }
}
