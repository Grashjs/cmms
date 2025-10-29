package com.grash.service;

import com.grash.dto.WorkflowActionPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.WorkflowActionMapper;
import com.grash.model.WorkflowAction;
import com.grash.repository.WorkflowActionRepository;
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
class WorkflowActionServiceTest {

    @Mock
    private WorkflowActionRepository workflowActionRepository;

    @Mock
    private WorkflowActionMapper workflowActionMapper;

    @InjectMocks
    private WorkflowActionService workflowActionService;

    private WorkflowAction workflowAction;

    @BeforeEach
    void setUp() {
        workflowAction = new WorkflowAction();
        workflowAction.setId(1L);
    }

    @Test
    void create() {
        when(workflowActionRepository.save(any(WorkflowAction.class))).thenReturn(workflowAction);

        WorkflowAction result = workflowActionService.create(workflowAction);

        assertNotNull(result);
        assertEquals(workflowAction.getId(), result.getId());
        verify(workflowActionRepository).save(workflowAction);
    }

    @Test
    void update_whenExists() {
        WorkflowActionPatchDTO patchDTO = new WorkflowActionPatchDTO();
        when(workflowActionRepository.existsById(1L)).thenReturn(true);
        when(workflowActionRepository.findById(1L)).thenReturn(Optional.of(workflowAction));
        when(workflowActionRepository.save(any(WorkflowAction.class))).thenReturn(workflowAction);
        when(workflowActionMapper.updateWorkflowAction(any(WorkflowAction.class), any(WorkflowActionPatchDTO.class))).thenReturn(workflowAction);

        WorkflowAction result = workflowActionService.update(1L, patchDTO);

        assertNotNull(result);
        assertEquals(workflowAction.getId(), result.getId());
        verify(workflowActionRepository).save(workflowAction);
    }

    @Test
    void update_whenNotExists_shouldThrowException() {
        WorkflowActionPatchDTO patchDTO = new WorkflowActionPatchDTO();
        when(workflowActionRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class, () -> workflowActionService.update(1L, patchDTO));

        assertEquals("Not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void getAll() {
        when(workflowActionRepository.findAll()).thenReturn(Collections.singletonList(workflowAction));

        assertEquals(1, workflowActionService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(workflowActionRepository).deleteById(1L);
        workflowActionService.delete(1L);
        verify(workflowActionRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(workflowActionRepository.findById(1L)).thenReturn(Optional.of(workflowAction));

        assertTrue(workflowActionService.findById(1L).isPresent());
    }
}
