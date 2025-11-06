package com.grash.controller;

import com.grash.dto.LaborPatchDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.enums.PlanFeatures;
import com.grash.model.enums.RoleType;
import com.grash.model.enums.Status;
import com.grash.model.enums.TimeStatus;
import com.grash.service.LaborService;
import com.grash.service.UserService;
import com.grash.service.WorkOrderService;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LaborControllerTest {

    @Mock
    private LaborService laborService;

    @Mock
    private UserService userService;

    @Mock
    private WorkOrderService workOrderService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private LaborController laborController;

    private OwnUser user;
    private WorkOrder workOrder;
    private Labor labor;

    @BeforeEach
    void setUp() {
        user = new OwnUser();
        user.setId(1L);
        user.setRate(10L);
        Role clientRole = new Role();
        clientRole.setRoleType(RoleType.ROLE_CLIENT);
        clientRole.setEditOtherPermissions(new HashSet<>());
        user.setRole(clientRole);

        Company company = new Company();
        Subscription subscription = new Subscription();
        SubscriptionPlan subscriptionPlan = new SubscriptionPlan();
        subscriptionPlan.setFeatures(new HashSet<>(Arrays.asList(PlanFeatures.ADDITIONAL_TIME)));
        subscription.setSubscriptionPlan(subscriptionPlan);
        company.setSubscription(subscription);
        user.setCompany(company);

        workOrder = new WorkOrder();
        workOrder.setId(1L);
        workOrder.setStatus(Status.OPEN);

        labor = new Labor();
        labor.setId(1L);
        labor.setWorkOrder(workOrder);
        labor.setAssignedTo(user);
    }

    @Nested
    @DisplayName("getById method")
    class GetByIdTests {

        @Test
        @DisplayName("Should return labor when found")
        void getById_found_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(laborService.findById(1L)).thenReturn(Optional.of(labor));

            Labor result = laborController.getById(1L, request);

            assertNotNull(result);
            assertEquals(labor.getId(), result.getId());
        }

        @Test
        @DisplayName("Should throw exception when labor not found")
        void getById_notFound_shouldThrowException() {
            when(userService.whoami(request)).thenReturn(user);
            when(laborService.findById(1L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class, () -> laborController.getById(1L, request));

            assertEquals("Not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("getByWorkOrder method")
    class GetByWorkOrderTests {

        @Test
        @DisplayName("Should return labors for a work order")
        void getByWorkOrder_found_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(workOrderService.findById(1L)).thenReturn(Optional.of(workOrder));
            when(laborService.findByWorkOrder(1L)).thenReturn(Collections.singletonList(labor));

            Collection<Labor> result = laborController.getByWorkOrder(1L, request);

            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("Should throw exception when work order not found")
        void getByWorkOrder_notFound_shouldThrowException() {
            when(userService.whoami(request)).thenReturn(user);
            when(workOrderService.findById(1L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class, () -> laborController.getByWorkOrder(1L, request));

            assertEquals("Not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("controlTimer method")
    class ControlTimerTests {

        @Test
        @DisplayName("Should start timer when no labor exists")
        void controlTimer_startNew_shouldSucceed() {
            workOrder.setAssignedTo(Collections.singletonList(user));
            when(userService.whoami(request)).thenReturn(user);
            when(workOrderService.findById(1L)).thenReturn(Optional.of(workOrder));
            when(laborService.findByWorkOrder(1L)).thenReturn(Collections.emptyList());
            when(laborService.create(any(Labor.class))).thenReturn(labor);

            Labor result = laborController.controlTimer(1L, request, true);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should stop running timer")
        void controlTimer_stopRunning_shouldSucceed() {
            labor.setLogged(true);
            labor.setStatus(TimeStatus.RUNNING);
            workOrder.setAssignedTo(Collections.singletonList(user));
            when(userService.whoami(request)).thenReturn(user);
            when(workOrderService.findById(1L)).thenReturn(Optional.of(workOrder));
            when(laborService.findByWorkOrder(1L)).thenReturn(Collections.singletonList(labor));
            when(laborService.stop(labor)).thenReturn(labor);

            Labor result = laborController.controlTimer(1L, request, false);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should throw exception when user cannot edit work order")
        void controlTimer_cannotEdit_shouldThrowException() {
            workOrder.setAssignedTo(Collections.emptyList()); // Not assigned
            workOrder.setCreatedBy(2L); // Not creator
            when(userService.whoami(request)).thenReturn(user);
            when(workOrderService.findById(1L)).thenReturn(Optional.of(workOrder));

            CustomException exception = assertThrows(CustomException.class, () -> laborController.controlTimer(1L, request, true));

            assertEquals("Not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("create method")
    class CreateTests {

        @Test
        @DisplayName("Should create labor with feature enabled")
        void create_withFeature_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(workOrderService.findById(anyLong())).thenReturn(Optional.of(workOrder));
            when(laborService.create(any(Labor.class))).thenReturn(labor);

            Labor result = laborController.create(labor, request);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should throw exception without feature enabled")
        void create_withoutFeature_shouldThrowException() {
            user.getCompany().getSubscription().getSubscriptionPlan().setFeatures(Collections.emptySet());
            when(userService.whoami(request)).thenReturn(user);

            CustomException exception = assertThrows(CustomException.class, () -> laborController.create(labor, request));

            assertEquals("Access denied", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("patch method")
    class PatchTests {

        private LaborPatchDTO patchDTO;

        @BeforeEach
        void patchSetup() {
            patchDTO = new LaborPatchDTO();
        }

        @Test
        @DisplayName("Should patch labor when found")
        void patch_found_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(laborService.findById(1L)).thenReturn(Optional.of(labor));
            when(laborService.update(anyLong(), any(LaborPatchDTO.class))).thenReturn(labor);

            Labor result = laborController.patch(patchDTO, 1L, request);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should throw exception when labor not found")
        void patch_notFound_shouldThrowException() {
            when(userService.whoami(request)).thenReturn(user);
            when(laborService.findById(1L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class, () -> laborController.patch(patchDTO, 1L, request));

            assertEquals("Labor not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("delete method")
    class DeleteTests {

        @Test
        @DisplayName("Should delete labor when found")
        void delete_found_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(laborService.findById(1L)).thenReturn(Optional.of(labor));

            ResponseEntity response = laborController.delete(1L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(((SuccessResponse) response.getBody()).isSuccess());
        }

        @Test
        @DisplayName("Should throw exception when labor not found")
        void delete_notFound_shouldThrowException() {
            when(userService.whoami(request)).thenReturn(user);
            when(laborService.findById(1L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class, () -> laborController.delete(1L, request));

            assertEquals("Labor not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }
    }
}