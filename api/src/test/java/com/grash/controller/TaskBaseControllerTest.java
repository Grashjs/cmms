package com.grash.controller;

import com.grash.dto.SuccessResponse;
import com.grash.dto.TaskBasePatchDTO;
import com.grash.exception.CustomException;
import com.grash.model.OwnUser;
import com.grash.model.TaskBase;
import com.grash.service.TaskBaseService;
import com.grash.service.UserService;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskBaseControllerTest {

    @Mock
    private TaskBaseService taskBaseService;

    @Mock
    private UserService userService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private TaskBaseController taskBaseController;

    private OwnUser user;
    private TaskBase taskBase;

    @BeforeEach
    void setUp() {
        user = new OwnUser();
        user.setId(1L);

        taskBase = new TaskBase();
        taskBase.setId(1L);
    }

    @Nested
    @DisplayName("getById method")
    class GetByIdTests {
        @Test
        @DisplayName("Should return task base when found")
        void getById_found_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(taskBaseService.findById(1L)).thenReturn(Optional.of(taskBase));

            TaskBase result = taskBaseController.getById(1L, request);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should throw exception when not found")
        void getById_notFound_shouldThrowException() {
            when(userService.whoami(request)).thenReturn(user);
            when(taskBaseService.findById(1L)).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> taskBaseController.getById(1L, request));
        }
    }

    @Nested
    @DisplayName("create method")
    class CreateTests {
        @Test
        @DisplayName("Should create and return task base")
        void create_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(taskBaseService.create(any(TaskBase.class))).thenReturn(taskBase);

            TaskBase result = taskBaseController.create(new TaskBase(), request);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("patch method")
    class PatchTests {
        @Test
        @DisplayName("Should patch and return task base when found")
        void patch_found_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(taskBaseService.findById(1L)).thenReturn(Optional.of(taskBase));
            when(taskBaseService.update(any(Long.class), any(TaskBasePatchDTO.class))).thenReturn(taskBase);

            TaskBase result = taskBaseController.patch(new TaskBasePatchDTO(), 1L, request);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should throw exception when not found")
        void patch_notFound_shouldThrowException() {
            when(userService.whoami(request)).thenReturn(user);
            when(taskBaseService.findById(1L)).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> taskBaseController.patch(new TaskBasePatchDTO(), 1L, request));
        }
    }

    @Nested
    @DisplayName("delete method")
    class DeleteTests {
        @Test
        @DisplayName("Should delete task base when found")
        void delete_found_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(taskBaseService.findById(1L)).thenReturn(Optional.of(taskBase));

            ResponseEntity response = taskBaseController.delete(1L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(((SuccessResponse) response.getBody()).isSuccess());
        }

        @Test
        @DisplayName("Should throw exception when not found")
        void delete_notFound_shouldThrowException() {
            when(userService.whoami(request)).thenReturn(user);
            when(taskBaseService.findById(1L)).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> taskBaseController.delete(1L, request));
        }
    }
}
