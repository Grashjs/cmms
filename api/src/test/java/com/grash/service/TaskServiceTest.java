package com.grash.service;

import com.grash.dto.TaskPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.TaskMapper;
import com.grash.model.Task;
import com.grash.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @InjectMocks
    private TaskService taskService;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private WorkOrderService workOrderService;

    @Mock
    private CompanyService companyService;

    @Mock
    private FileService fileService;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private EntityManager em;

    private Task task;
    private TaskPatchDTO taskPatchDTO;

    @BeforeEach
    void setUp() {
        task = new Task();
        task.setId(1L);
        task.setNotes("Test Notes"); // Corrected field

        taskPatchDTO = new TaskPatchDTO();
        taskPatchDTO.setNotes("Updated Notes"); // Corrected field
    }

    @Nested
    @DisplayName("create method")
    class Create {
        @Test
        @DisplayName("should create and return a new task")
        void shouldCreateAndReturnNewTask() {
            when(taskRepository.saveAndFlush(any(Task.class))).thenReturn(task);
            doNothing().when(em).refresh(any(Task.class));

            Task createdTask = taskService.create(task);

            assertNotNull(createdTask);
            assertEquals(task.getNotes(), createdTask.getNotes()); // Corrected field
            verify(taskRepository, times(1)).saveAndFlush(any(Task.class));
            verify(em, times(1)).refresh(any(Task.class));
        }
    }

    @Nested
    @DisplayName("update method")
    class Update {
        @Test
        @DisplayName("should update an existing task")
        void shouldUpdateExistingTask() {
            when(taskRepository.existsById(anyLong())).thenReturn(true);
            when(taskRepository.findById(anyLong())).thenReturn(Optional.of(task));
            when(taskMapper.updateTask(any(Task.class), any(TaskPatchDTO.class))).thenReturn(task);
            when(taskRepository.saveAndFlush(any(Task.class))).thenReturn(task);
            doNothing().when(em).refresh(any(Task.class));

            Task updatedTask = taskService.update(1L, taskPatchDTO);

            assertNotNull(updatedTask);
            assertEquals(task.getNotes(), updatedTask.getNotes()); // Corrected field
            verify(taskRepository, times(1)).existsById(1L);
            verify(taskRepository, times(1)).findById(1L);
            verify(taskMapper, times(1)).updateTask(task, taskPatchDTO);
            verify(taskRepository, times(1)).saveAndFlush(task);
            verify(em, times(1)).refresh(task);
        }

        @Test
        @DisplayName("should throw CustomException when task not found")
        void shouldThrowCustomExceptionWhenTaskNotFound() {
            when(taskRepository.existsById(anyLong())).thenReturn(false);

            CustomException exception = assertThrows(CustomException.class, () -> {
                taskService.update(1L, taskPatchDTO);
            });

            assertEquals("Not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
            verify(taskRepository, times(1)).existsById(1L);
            verify(taskRepository, never()).findById(anyLong());
            verify(taskMapper, never()).updateTask(any(Task.class), any(TaskPatchDTO.class));
            verify(taskRepository, never()).saveAndFlush(any(Task.class));
            verify(em, never()).refresh(any(Task.class));
        }
    }

    @Nested
    @DisplayName("getAll method")
    class GetAll {
        @Test
        @DisplayName("should return a collection of all tasks")
        void shouldReturnCollectionOfAllTasks() {
            List<Task> tasks = Arrays.asList(task, new Task());
            when(taskRepository.findAll()).thenReturn(tasks);

            Collection<Task> result = taskService.getAll();

            assertNotNull(result);
            assertEquals(2, result.size());
            verify(taskRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("should return an empty collection when no tasks exist")
        void shouldReturnEmptyCollectionWhenNoTasksExist() {
            when(taskRepository.findAll()).thenReturn(Collections.emptyList());

            Collection<Task> result = taskService.getAll();

            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(taskRepository, times(1)).findAll();
        }
    }

    @Nested
    @DisplayName("delete method")
    class Delete {
        @Test
        @DisplayName("should delete a task by id")
        void shouldDeleteTaskById() {
            doNothing().when(taskRepository).deleteById(anyLong());

            taskService.delete(1L);

            verify(taskRepository, times(1)).deleteById(1L);
        }
    }

    @Nested
    @DisplayName("findById method")
    class FindById {
        @Test
        @DisplayName("should return an Optional with task if found")
        void shouldReturnOptionalWithTaskIfFound() {
            when(taskRepository.findById(anyLong())).thenReturn(Optional.of(task));

            Optional<Task> result = taskService.findById(1L);

            assertTrue(result.isPresent());
            assertEquals(task.getNotes(), result.get().getNotes()); // Corrected field
            verify(taskRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("should return an empty Optional if task not found")
        void shouldReturnEmptyOptionalIfTaskNotFound() {
            when(taskRepository.findById(anyLong())).thenReturn(Optional.empty());

            Optional<Task> result = taskService.findById(1L);

            assertFalse(result.isPresent());
            verify(taskRepository, times(1)).findById(1L);
        }
    }

    @Nested
    @DisplayName("findByWorkOrder method")
    class FindByWorkOrder {
        @Test
        @DisplayName("should return a list of tasks for a given work order ID")
        void shouldReturnListOfTasksForWorkOrderId() {
            List<Task> tasks = Collections.singletonList(task);
            when(taskRepository.findByWorkOrder_Id(anyLong())).thenReturn(tasks);

            List<Task> result = taskService.findByWorkOrder(1L);

            assertNotNull(result);
            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
            assertEquals(task.getNotes(), result.get(0).getNotes()); // Corrected field
            verify(taskRepository, times(1)).findByWorkOrder_Id(1L);
        }

        @Test
        @DisplayName("should return an empty list if no tasks found for work order ID")
        void shouldReturnEmptyListIfNoTasksFoundForWorkOrderId() {
            when(taskRepository.findByWorkOrder_Id(anyLong())).thenReturn(Collections.emptyList());

            List<Task> result = taskService.findByWorkOrder(1L);

            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(taskRepository, times(1)).findByWorkOrder_Id(1L);
        }
    }

    @Nested
    @DisplayName("findByPreventiveMaintenance method")
    class FindByPreventiveMaintenance {
        @Test
        @DisplayName("should return a list of tasks for a given preventive maintenance ID")
        void shouldReturnListOfTasksForPreventiveMaintenanceId() {
            List<Task> tasks = Collections.singletonList(task);
            when(taskRepository.findByPreventiveMaintenance_Id(anyLong())).thenReturn(tasks);

            List<Task> result = taskService.findByPreventiveMaintenance(1L);

            assertNotNull(result);
            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
            assertEquals(task.getNotes(), result.get(0).getNotes()); // Corrected field
            verify(taskRepository, times(1)).findByPreventiveMaintenance_Id(1L);
        }

        @Test
        @DisplayName("should return an empty list if no tasks found for preventive maintenance ID")
        void shouldReturnEmptyListIfNoTasksFoundForPreventiveMaintenanceId() {
            when(taskRepository.findByPreventiveMaintenance_Id(anyLong())).thenReturn(Collections.emptyList());

            List<Task> result = taskService.findByPreventiveMaintenance(1L);

            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(taskRepository, times(1)).findByPreventiveMaintenance_Id(1L);
        }
    }
}