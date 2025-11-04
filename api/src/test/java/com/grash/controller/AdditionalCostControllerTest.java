package com.grash.controller;

import com.grash.dto.AdditionalCostPatchDTO;
import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.enums.PlanFeatures;
import com.grash.service.AdditionalCostService;
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
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdditionalCostControllerTest {

    @Mock
    private AdditionalCostService additionalCostService;
    @Mock
    private UserService userService;
    @Mock
    private WorkOrderService workOrderService;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AdditionalCostController additionalCostController;

    private OwnUser user;
    private AdditionalCost additionalCost;
    private WorkOrder workOrder;
    private Company company;
    private Subscription subscription;
    private SubscriptionPlan subscriptionPlan;

    @BeforeEach
    void setUp() {
        subscriptionPlan = new SubscriptionPlan();
        subscriptionPlan.setFeatures(new HashSet<>());

        subscription = new Subscription();
        subscription.setSubscriptionPlan(subscriptionPlan);

        company = new Company();
        company.setId(1L);
        company.setSubscription(subscription);

        user = new OwnUser();
        user.setId(1L);
        user.setCompany(company);

        workOrder = new WorkOrder();
        workOrder.setId(1L);

        additionalCost = new AdditionalCost();
        additionalCost.setId(1L);
        additionalCost.setWorkOrder(workOrder);
    }

    @Nested
    @DisplayName("Get By Id Tests")
    class GetByIdTests {

        @Test
        @DisplayName("Should throw not found when additional cost does not exist")
        void shouldThrowNotFoundWhenAdditionalCostDoesNotExist() {
            when(userService.whoami(request)).thenReturn(user);
            when(additionalCostService.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> additionalCostController.getById(1L, request));
        }

        @Test
        @DisplayName("Should return additional cost by id")
        void shouldReturnAdditionalCostById() {
            when(userService.whoami(request)).thenReturn(user);
            when(additionalCostService.findById(anyLong())).thenReturn(Optional.of(additionalCost));

            AdditionalCost result = additionalCostController.getById(1L, request);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Get By Work Order Tests")
    class GetByWorkOrderTests {

        @Test
        @DisplayName("Should throw not found when work order does not exist")
        void shouldThrowNotFoundWhenWorkOrderDoesNotExist() {
            when(userService.whoami(request)).thenReturn(user);
            when(workOrderService.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> additionalCostController.getByWorkOrder(1L, request));
        }

        @Test
        @DisplayName("Should return additional costs by work order")
        void shouldReturnAdditionalCostsByWorkOrder() {
            when(userService.whoami(request)).thenReturn(user);
            when(workOrderService.findById(anyLong())).thenReturn(Optional.of(workOrder));
            when(additionalCostService.findByWorkOrder(anyLong())).thenReturn(Collections.singletonList(additionalCost));

            java.util.Collection<AdditionalCost> result = additionalCostController.getByWorkOrder(1L, request);

            assertFalse(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Create Tests")
    class CreateTests {

        @Test
        @DisplayName("Should throw forbidden if subscription has no feature")
        void shouldThrowForbiddenIfSubscriptionHasNoFeature() {
            when(userService.whoami(request)).thenReturn(user);

            assertThrows(CustomException.class, () -> additionalCostController.create(additionalCost, request));
        }

        @Test
        @DisplayName("Should create additional cost and set first time to react")
        void shouldCreateAdditionalCostAndSetFirstTimeToReact() {
            subscriptionPlan.getFeatures().add(PlanFeatures.ADDITIONAL_COST);
            when(userService.whoami(request)).thenReturn(user);
            when(workOrderService.findById(anyLong())).thenReturn(Optional.of(workOrder));
            when(additionalCostService.create(any(AdditionalCost.class))).thenReturn(additionalCost);

            AdditionalCost result = additionalCostController.create(additionalCost, request);

            assertNotNull(result);
            verify(workOrderService).save(workOrder);
            assertNotNull(workOrder.getFirstTimeToReact());
        }

        @Test
        @DisplayName("Should create additional cost without setting first time to react")
        void shouldCreateAdditionalCostWithoutSettingFirstTimeToReact() {
            workOrder.setFirstTimeToReact(new Date());
            subscriptionPlan.getFeatures().add(PlanFeatures.ADDITIONAL_COST);
            when(userService.whoami(request)).thenReturn(user);
            when(workOrderService.findById(anyLong())).thenReturn(Optional.of(workOrder));
            when(additionalCostService.create(any(AdditionalCost.class))).thenReturn(additionalCost);

            AdditionalCost result = additionalCostController.create(additionalCost, request);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Patch Tests")
    class PatchTests {

        private AdditionalCostPatchDTO patchDTO;

        @BeforeEach
        void setup() {
            patchDTO = new AdditionalCostPatchDTO();
        }

        @Test
        @DisplayName("Should throw not found when additional cost does not exist")
        void shouldThrowNotFoundWhenAdditionalCostDoesNotExist() {
            when(userService.whoami(request)).thenReturn(user);
            when(additionalCostService.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> additionalCostController.patch(patchDTO, 1L, request));
        }

        @Test
        @DisplayName("Should patch additional cost successfully")
        void shouldPatchAdditionalCostSuccessfully() {
            when(userService.whoami(request)).thenReturn(user);
            when(additionalCostService.findById(anyLong())).thenReturn(Optional.of(additionalCost));
            when(additionalCostService.update(anyLong(), any(AdditionalCostPatchDTO.class))).thenReturn(additionalCost);

            AdditionalCost result = additionalCostController.patch(patchDTO, 1L, request);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Delete Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should throw not found on delete")
        void shouldThrowNotFoundOnDelete() {
            when(userService.whoami(request)).thenReturn(user);
            when(additionalCostService.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> additionalCostController.delete(1L, request));
        }

        @Test
        @DisplayName("Should delete additional cost successfully")
        void shouldDeleteAdditionalCostSuccessfully() {
            when(userService.whoami(request)).thenReturn(user);
            when(additionalCostService.findById(anyLong())).thenReturn(Optional.of(additionalCost));

            ResponseEntity response = additionalCostController.delete(1L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(additionalCostService).delete(1L);
        }
    }
}
