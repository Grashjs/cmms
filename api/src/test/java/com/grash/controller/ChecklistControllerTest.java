package com.grash.controller;

import com.grash.dto.ChecklistPatchDTO;
import com.grash.dto.ChecklistPostDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PlanFeatures;
import com.grash.model.enums.RoleType;
import com.grash.service.ChecklistService;
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
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChecklistControllerTest {

    @Mock
    private ChecklistService checklistService;
    @Mock
    private UserService userService;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private ChecklistController checklistController;

    private OwnUser user;
    private Checklist checklist;
    private Company company;
    private CompanySettings companySettings;
    private Role role;
    private Subscription subscription;
    private SubscriptionPlan subscriptionPlan;

    @BeforeEach
    void setUp() {
        companySettings = new CompanySettings();
        companySettings.setId(1L);

        subscriptionPlan = new SubscriptionPlan();
        subscriptionPlan.setFeatures(new HashSet<>());

        subscription = new Subscription();
        subscription.setSubscriptionPlan(subscriptionPlan);

        company = new Company();
        company.setId(1L);
        company.setCompanySettings(companySettings);
        company.setSubscription(subscription);

        role = new Role();
        role.setRoleType(RoleType.ROLE_CLIENT);
        role.setViewPermissions(new HashSet<>());

        user = new OwnUser();
        user.setId(1L);
        user.setCompany(company);
        user.setRole(role);

        checklist = new Checklist();
        checklist.setId(1L);
    }

    @Nested
    @DisplayName("Get All Checklists Tests")
    class GetAllChecklistsTests {

        @Test
        @DisplayName("Should return checklists for client user")
        void shouldReturnChecklistsForClientUser() {
            when(userService.whoami(request)).thenReturn(user);
            when(checklistService.findByCompanySettings(anyLong())).thenReturn(Collections.singletonList(checklist));

            Collection<Checklist> result = checklistController.getAll(request);

            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("Should return all checklists for non-client user")
        void shouldReturnAllChecklistsForNonClientUser() {
            role.setRoleType(RoleType.ROLE_SUPER_ADMIN);
            when(userService.whoami(request)).thenReturn(user);
            when(checklistService.getAll()).thenReturn(Collections.singletonList(checklist));

            Collection<Checklist> result = checklistController.getAll(request);

            assertFalse(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Get By Id Tests")
    class GetByIdTests {

        @Test
        @DisplayName("Should throw not found when checklist does not exist")
        void shouldThrowNotFoundWhenChecklistDoesNotExist() {
            when(userService.whoami(request)).thenReturn(user);
            when(checklistService.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> checklistController.getById(1L, request));
        }

        @Test
        @DisplayName("Should return checklist by id")
        void shouldReturnChecklistById() {
            when(userService.whoami(request)).thenReturn(user);
            when(checklistService.findById(anyLong())).thenReturn(Optional.of(checklist));

            Checklist result = checklistController.getById(1L, request);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Create Tests")
    class CreateTests {

        private ChecklistPostDTO checklistPostDTO;

        @BeforeEach
        void setup() {
            checklistPostDTO = new ChecklistPostDTO();
        }

        @Test
        @DisplayName("Should throw forbidden if user has no permission")
        void shouldThrowForbiddenIfUserHasNoPermission() {
            when(userService.whoami(request)).thenReturn(user);

            assertThrows(CustomException.class, () -> checklistController.create(checklistPostDTO, request));
        }

        @Test
        @DisplayName("Should throw forbidden if subscription has no feature")
        void shouldThrowForbiddenIfSubscriptionHasNoFeature() {
            role.getViewPermissions().add(PermissionEntity.SETTINGS);
            when(userService.whoami(request)).thenReturn(user);

            assertThrows(CustomException.class, () -> checklistController.create(checklistPostDTO, request));
        }

        @Test
        @DisplayName("Should create checklist successfully")
        void shouldCreateChecklistSuccessfully() {
            role.getViewPermissions().add(PermissionEntity.SETTINGS);
            subscriptionPlan.getFeatures().add(PlanFeatures.CHECKLIST);
            when(userService.whoami(request)).thenReturn(user);
            when(checklistService.createPost(any(ChecklistPostDTO.class), any(Company.class))).thenReturn(checklist);

            Checklist result = checklistController.create(checklistPostDTO, request);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Patch Tests")
    class PatchTests {

        private ChecklistPatchDTO checklistPatchDTO;

        @BeforeEach
        void setup() {
            checklistPatchDTO = new ChecklistPatchDTO();
        }

        @Test
        @DisplayName("Should throw not found when checklist does not exist")
        void shouldThrowNotFoundWhenChecklistDoesNotExist() {
            when(userService.whoami(request)).thenReturn(user);
            when(checklistService.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> checklistController.patch(checklistPatchDTO, 1L, request));
        }

        @Test
        @DisplayName("Should throw forbidden if user has no permission")
        void shouldThrowForbiddenIfUserHasNoPermission() {
            when(userService.whoami(request)).thenReturn(user);
            when(checklistService.findById(anyLong())).thenReturn(Optional.of(checklist));

            assertThrows(CustomException.class, () -> checklistController.patch(checklistPatchDTO, 1L, request));
        }

        @Test
        @DisplayName("Should patch checklist successfully")
        void shouldPatchChecklistSuccessfully() {
            role.getViewPermissions().add(PermissionEntity.SETTINGS);
            when(userService.whoami(request)).thenReturn(user);
            when(checklistService.findById(anyLong())).thenReturn(Optional.of(checklist));
            when(checklistService.update(anyLong(), any(ChecklistPatchDTO.class), any(Company.class))).thenReturn(checklist);

            Checklist result = checklistController.patch(checklistPatchDTO, 1L, request);

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
            when(checklistService.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> checklistController.delete(1L, request));
        }

        @Test
        @DisplayName("Should throw forbidden if user has no permission")
        void shouldThrowForbiddenIfUserHasNoPermission() {
            when(userService.whoami(request)).thenReturn(user);
            when(checklistService.findById(anyLong())).thenReturn(Optional.of(checklist));

            assertThrows(CustomException.class, () -> checklistController.delete(1L, request));
        }

        @Test
        @DisplayName("Should delete checklist successfully")
        void shouldDeleteChecklistSuccessfully() {
            role.getViewPermissions().add(PermissionEntity.SETTINGS);
            when(userService.whoami(request)).thenReturn(user);
            when(checklistService.findById(anyLong())).thenReturn(Optional.of(checklist));

            ResponseEntity<SuccessResponse> response = checklistController.delete(1L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(checklistService).delete(1L);
        }
    }
}