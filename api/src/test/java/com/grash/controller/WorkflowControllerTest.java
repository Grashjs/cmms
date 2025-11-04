package com.grash.controller;

import com.grash.dto.SuccessResponse;
import com.grash.dto.WorkflowActionPostDTO;
import com.grash.dto.WorkflowPostDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.WorkflowActionMapper;
import com.grash.mapper.WorkflowConditionMapper;
import com.grash.model.*;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PlanFeatures;
import com.grash.model.enums.RoleType;
import com.grash.service.UserService;
import com.grash.service.WorkflowActionService;
import com.grash.service.WorkflowConditionService;
import com.grash.service.WorkflowService;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowControllerTest {

    @Mock
    private WorkflowService workflowService;
    @Mock
    private UserService userService;
    @Mock
    private WorkflowConditionMapper workflowConditionMapper;
    @Mock
    private WorkflowConditionService workflowConditionService;
    @Mock
    private WorkflowActionMapper workflowActionMapper;
    @Mock
    private WorkflowActionService workflowActionService;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private WorkflowController workflowController;

    private OwnUser user;
    private Workflow workflow;
    private Company company;
    private Role role;
    private Subscription subscription;
    private SubscriptionPlan subscriptionPlan;
    private WorkflowPostDTO workflowPostDTO;

    @BeforeEach
    void setUp() {
        subscriptionPlan = new SubscriptionPlan();
        subscriptionPlan.setFeatures(new HashSet<>());

        subscription = new Subscription();
        subscription.setSubscriptionPlan(subscriptionPlan);

        company = new Company();
        company.setId(1L);
        company.setSubscription(subscription);

        role = new Role();
        role.setRoleType(RoleType.ROLE_CLIENT);
        role.setViewPermissions(new HashSet<>());

        user = new OwnUser();
        user.setId(1L);
        user.setCompany(company);
        user.setRole(role);

        workflow = new Workflow();
        workflow.setId(1L);
        workflow.setEnabled(true);

        workflowPostDTO = new WorkflowPostDTO();
        workflowPostDTO.setSecondaryConditions(Collections.emptyList());
        workflowPostDTO.setAction(new WorkflowActionPostDTO());
    }

    @Nested
    @DisplayName("Get All Workflows Tests")
    class GetAllWorkflowsTests {

        @Test
        @DisplayName("Should return workflows for client user")
        void shouldReturnWorkflowsForClientUser() {
            when(userService.whoami(request)).thenReturn(user);
            when(workflowService.findByCompany(anyLong())).thenReturn(Collections.singletonList(workflow));

            Collection<Workflow> result = workflowController.getAll(request);

            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("Should return all workflows for non-client user")
        void shouldReturnAllWorkflowsForNonClientUser() {
            role.setRoleType(RoleType.ROLE_SUPER_ADMIN);
            when(userService.whoami(request)).thenReturn(user);
            when(workflowService.getAll()).thenReturn(Collections.singletonList(workflow));

            Collection<Workflow> result = workflowController.getAll(request);

            assertFalse(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Create Workflow Tests")
    class CreateWorkflowTests {

        @Test
        @DisplayName("Should throw forbidden if user has no permission")
        void shouldThrowForbiddenIfUserHasNoPermission() {
            when(userService.whoami(request)).thenReturn(user);

            assertThrows(CustomException.class, () -> workflowController.create(workflowPostDTO, request));
        }

        @Test
        @DisplayName("Should throw not acceptable if no workflow feature and workflows exist")
        void shouldThrowNotAcceptableIfNoWorkflowFeatureAndWorkflowsExist() {
            role.getViewPermissions().add(PermissionEntity.SETTINGS);
            when(userService.whoami(request)).thenReturn(user);
            when(workflowService.findByCompany(anyLong())).thenReturn(Arrays.asList(workflow));

            assertThrows(CustomException.class, () -> workflowController.create(workflowPostDTO, request));
        }

        @Test
        @DisplayName("Should create workflow successfully with feature")
        void shouldCreateWorkflowSuccessfullyWithFeature() {
            role.getViewPermissions().add(PermissionEntity.SETTINGS);
            subscriptionPlan.getFeatures().add(PlanFeatures.WORKFLOW);
            when(userService.whoami(request)).thenReturn(user);
            when(workflowService.findByCompany(anyLong())).thenReturn(Collections.emptyList());
            when(workflowConditionService.saveAll(any())).thenReturn(Collections.emptyList());
            when(workflowActionMapper.toModel(any(WorkflowActionPostDTO.class))).thenReturn(new WorkflowAction());
            when(workflowActionService.create(any(WorkflowAction.class))).thenReturn(new WorkflowAction());
            when(workflowService.create(any(Workflow.class))).thenReturn(workflow);

            Workflow result = workflowController.create(workflowPostDTO, request);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should create workflow successfully without feature if no workflows exist")
        void shouldCreateWorkflowSuccessfullyWithoutFeatureIfNoWorkflowsExist() {
            role.getViewPermissions().add(PermissionEntity.SETTINGS);
            when(userService.whoami(request)).thenReturn(user);
            when(workflowService.findByCompany(anyLong())).thenReturn(Collections.emptyList());
            when(workflowConditionService.saveAll(any())).thenReturn(Collections.emptyList());
            when(workflowActionMapper.toModel(any(WorkflowActionPostDTO.class))).thenReturn(new WorkflowAction());
            when(workflowActionService.create(any(WorkflowAction.class))).thenReturn(new WorkflowAction());
            when(workflowService.create(any(Workflow.class))).thenReturn(workflow);

            Workflow result = workflowController.create(workflowPostDTO, request);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Get By Id Tests")
    class GetByIdTests {

        @Test
        @DisplayName("Should throw not found when workflow does not exist")
        void shouldThrowNotFoundWhenWorkflowDoesNotExist() {
            when(userService.whoami(request)).thenReturn(user);
            when(workflowService.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> workflowController.getById(1L, request));
        }

        @Test
        @DisplayName("Should return workflow by id")
        void shouldReturnWorkflowById() {
            when(userService.whoami(request)).thenReturn(user);
            when(workflowService.findById(anyLong())).thenReturn(Optional.of(workflow));

            Workflow result = workflowController.getById(1L, request);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Patch Tests")
    class PatchTests {

        @Test
        @DisplayName("Should throw not found when workflow does not exist")
        void shouldThrowNotFoundWhenWorkflowDoesNotExist() {
            when(userService.whoami(request)).thenReturn(user);
            when(workflowService.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> workflowController.patch(workflowPostDTO, 1L, request));
        }

        @Test
        @DisplayName("Should patch workflow successfully")
        void shouldPatchWorkflowSuccessfully() {
            when(userService.whoami(request)).thenReturn(user);
            when(workflowService.findById(anyLong())).thenReturn(Optional.of(workflow));
            when(workflowConditionService.saveAll(any())).thenReturn(Collections.emptyList());
            when(workflowActionMapper.toModel(any(WorkflowActionPostDTO.class))).thenReturn(new WorkflowAction());
            when(workflowActionService.create(any(WorkflowAction.class))).thenReturn(new WorkflowAction());
            when(workflowService.create(any(Workflow.class))).thenReturn(workflow);

            Workflow result = workflowController.patch(workflowPostDTO, 1L, request);

            assertNotNull(result);
            verify(workflowService).delete(1L);
        }
    }

    @Nested
    @DisplayName("Delete Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should throw not found on delete")
        void shouldThrowNotFoundOnDelete() {
            when(userService.whoami(request)).thenReturn(user);
            when(workflowService.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> workflowController.delete(1L, request));
        }

        @Test
        @DisplayName("Should delete workflow successfully")
        void shouldDeleteWorkflowSuccessfully() {
            when(userService.whoami(request)).thenReturn(user);
            when(workflowService.findById(anyLong())).thenReturn(Optional.of(workflow));

            ResponseEntity<SuccessResponse> response = workflowController.delete(1L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(workflowService).delete(1L);
        }
    }
}