package com.grash.controller;

import com.grash.dto.SuccessResponse;
import com.grash.dto.TaskBaseDTO;
import com.grash.dto.TaskPatchDTO;
import com.grash.dto.TaskShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.TaskMapper;
import com.grash.model.*;
import com.grash.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    @Mock
    private TaskService taskService;
    @Mock
    private UserService userService;
    @Mock
    private TaskBaseService taskBaseService;
    @Mock
    private WorkOrderService workOrderService;
    @Mock
    private WorkflowService workflowService;
    @Mock
    private TaskMapper taskMapper;
    @Mock
    private PreventiveMaintenanceService preventiveMaintenanceService;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private TaskController taskController;

    private OwnUser user;
    private Task task;
    private TaskShowDTO taskShowDTO;
    private WorkOrder workOrder;
    private PreventiveMaintenance preventiveMaintenance;

    @BeforeEach
    void setUp() {
        user = new OwnUser();
        user.setId(1L);
        user.setCompany(new Company());

        task = new Task();
        task.setId(1L);

        taskShowDTO = new TaskShowDTO();
        taskShowDTO.setId(1L);

        workOrder = new WorkOrder();
        workOrder.setId(1L);

        preventiveMaintenance = new PreventiveMaintenance();
        preventiveMaintenance.setId(1L);
    }

    @Nested
    @DisplayName("getById method")
    class GetByIdTests {
        @Test
        @DisplayName("Should return task when found")
        void getById_found_shouldSucceed() {
            when(taskService.findById(1L)).thenReturn(Optional.of(task));
            when(taskMapper.toShowDto(task)).thenReturn(taskShowDTO);

            TaskShowDTO result = taskController.getById(1L, request);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should throw exception when not found")
        void getById_notFound_shouldThrowException() {
            when(taskService.findById(1L)).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> taskController.getById(1L, request));
        }
    }

    @Nested
    @DisplayName("getByWorkOrder method")
    class GetByWorkOrderTests {
        @Test
        @DisplayName("Should return tasks when work order found")
        void getByWorkOrder_found_shouldSucceed() {
            when(workOrderService.findById(1L)).thenReturn(Optional.of(workOrder));
            when(taskService.findByWorkOrder(1L)).thenReturn(Collections.singletonList(task));
            when(taskMapper.toShowDto(task)).thenReturn(taskShowDTO);

            Collection<TaskShowDTO> result = taskController.getByWorkOrder(1L, request);

            assertFalse(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getByPreventiveMaintenance method")
    class GetByPreventiveMaintenanceTests {
        @Test
        @DisplayName("Should return tasks when preventive maintenance found")
        void getByPreventiveMaintenance_found_shouldSucceed() {
            when(preventiveMaintenanceService.findById(1L)).thenReturn(Optional.of(preventiveMaintenance));
            when(taskService.findByPreventiveMaintenance(1L)).thenReturn(Collections.singletonList(task));

            Collection<Task> result = taskController.getByPreventiveMaintenance(1L);

            assertFalse(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("patch method")
    class PatchTests {
        @Test
        @DisplayName("Should patch and return task when found")
        void patch_found_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(taskService.findById(1L)).thenReturn(Optional.of(task));
            when(taskService.update(any(Long.class), any(TaskPatchDTO.class))).thenReturn(task);
            when(taskMapper.toShowDto(task)).thenReturn(taskShowDTO);

            TaskShowDTO result = taskController.patch(new TaskPatchDTO(), 1L, request);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("delete method")
    class DeleteTests {
        @Test
        @DisplayName("Should delete task when found")
        void delete_found_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(taskService.findById(1L)).thenReturn(Optional.of(task));

            ResponseEntity response = taskController.delete(1L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(((SuccessResponse) response.getBody()).isSuccess());
        }
    }
}
