package com.grash.controller;

import com.grash.dto.SchedulePatchDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.Company;
import com.grash.model.OwnUser;
import com.grash.model.Role;
import com.grash.model.Schedule;
import com.grash.model.enums.RoleType;
import com.grash.service.ScheduleService;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleControllerTest {

    @Mock
    private ScheduleService scheduleService;

    @Mock
    private UserService userService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private ScheduleController scheduleController;

    private OwnUser user;
    private Schedule schedule;

    @BeforeEach
    void setUp() {
        Company company = new Company();
        company.setId(1L);

        Role role = new Role();
        role.setRoleType(RoleType.ROLE_CLIENT);

        user = new OwnUser();
        user.setId(1L);
        user.setCompany(company);
        user.setRole(role);

        schedule = new Schedule();
        schedule.setId(1L);
    }

    @Nested
    @DisplayName("getAll method")
    class GetAllTests {
        @Test
        @DisplayName("Should return schedules for client user")
        void getAll_clientUser_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(scheduleService.findByCompany(1L)).thenReturn(Collections.singletonList(schedule));

            Collection<Schedule> result = scheduleController.getAll(request);

            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("Should return all schedules for non-client user")
        void getAll_nonClientUser_shouldSucceed() {
            user.getRole().setRoleType(RoleType.ROLE_SUPER_ADMIN);
            when(userService.whoami(request)).thenReturn(user);
            when(scheduleService.getAll()).thenReturn(Collections.singletonList(schedule));

            Collection<Schedule> result = scheduleController.getAll(request);

            assertFalse(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getById method")
    class GetByIdTests {
        @Test
        @DisplayName("Should return schedule when found")
        void getById_found_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(scheduleService.findById(1L)).thenReturn(Optional.of(schedule));

            Schedule result = scheduleController.getById(1L, request);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should throw exception when not found")
        void getById_notFound_shouldThrowException() {
            when(userService.whoami(request)).thenReturn(user);
            when(scheduleService.findById(1L)).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> scheduleController.getById(1L, request));
        }
    }

    @Nested
    @DisplayName("patch method")
    class PatchTests {
        @Test
        @DisplayName("Should patch and return schedule when found")
        void patch_found_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(scheduleService.findById(1L)).thenReturn(Optional.of(schedule));
            when(scheduleService.update(any(Long.class), any(SchedulePatchDTO.class))).thenReturn(schedule);

            Schedule result = scheduleController.patch(new SchedulePatchDTO(), 1L, request);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("delete method")
    class DeleteTests {
        @Test
        @DisplayName("Should delete schedule when found")
        void delete_found_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(scheduleService.findById(1L)).thenReturn(Optional.of(schedule));

            ResponseEntity response = scheduleController.delete(1L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(((SuccessResponse) response.getBody()).isSuccess());
        }
    }
}
