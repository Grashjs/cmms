package com.grash.controller;

import com.grash.dto.RolePatchDTO;
import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PlanFeatures;
import com.grash.model.enums.RoleType;
import com.grash.service.RoleService;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleControllerTest {

    @Mock
    private RoleService roleService;
    @Mock
    private UserService userService;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private RoleController roleController;

    private OwnUser user;
    private Role role;
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

        role = new Role();
        role.setRoleType(RoleType.ROLE_CLIENT);
        role.setViewPermissions(new HashSet<>());

        user = new OwnUser();
        user.setId(1L);
        user.setCompany(company);
        user.setRole(role);
    }

    @Nested
    @DisplayName("Get All Roles Tests")
    class GetAllRolesTests {

        @Test
        @DisplayName("Should throw forbidden if client has no permission")
        void shouldThrowForbiddenIfClientHasNoPermission() {
            when(userService.whoami(request)).thenReturn(user);

            assertThrows(CustomException.class, () -> roleController.getAll(request));
        }

        @Test
        @DisplayName("Should return roles for client with permission")
        void shouldReturnRolesForClientWithPermission() {
            role.getViewPermissions().add(PermissionEntity.SETTINGS);
            when(userService.whoami(request)).thenReturn(user);
            when(roleService.findByCompany(anyLong())).thenReturn(Collections.singletonList(role));

            java.util.Collection<Role> result = roleController.getAll(request);

            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("Should return all roles for non-client user")
        void shouldReturnAllRolesForNonClientUser() {
            role.setRoleType(RoleType.ROLE_SUPER_ADMIN);
            when(userService.whoami(request)).thenReturn(user);
            when(roleService.getAll()).thenReturn(Collections.singletonList(role));

            java.util.Collection<Role> result = roleController.getAll(request);

            assertFalse(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Get Role By Id Tests")
    class GetRoleByIdTests {

        @Test
        @DisplayName("Should throw not found when role does not exist")
        void shouldThrowNotFoundWhenRoleDoesNotExist() {
            when(userService.whoami(request)).thenReturn(user);
            when(roleService.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> roleController.getById(1L, request));
        }

        @Test
        @DisplayName("Should throw forbidden if user has no permission")
        void shouldThrowForbiddenIfUserHasNoPermission() {
            when(userService.whoami(request)).thenReturn(user);
            when(roleService.findById(anyLong())).thenReturn(Optional.of(role));

            assertThrows(CustomException.class, () -> roleController.getById(1L, request));
        }

        @Test
        @DisplayName("Should return role by id")
        void shouldReturnRoleById() {
            role.getViewPermissions().add(PermissionEntity.SETTINGS);
            when(userService.whoami(request)).thenReturn(user);
            when(roleService.findById(anyLong())).thenReturn(Optional.of(role));

            Role result = roleController.getById(1L, request);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Create Role Tests")
    class CreateRoleTests {

        @Test
        @DisplayName("Should throw forbidden if user has no permission")
        void shouldThrowForbiddenIfUserHasNoPermission() {
            when(userService.whoami(request)).thenReturn(user);

            assertThrows(CustomException.class, () -> roleController.create(role, request));
        }

        @Test
        @DisplayName("Should throw forbidden if subscription has no feature")
        void shouldThrowForbiddenIfSubscriptionHasNoFeature() {
            role.getViewPermissions().add(PermissionEntity.SETTINGS);
            when(userService.whoami(request)).thenReturn(user);

            assertThrows(CustomException.class, () -> roleController.create(role, request));
        }

        @Test
        @DisplayName("Should create role successfully")
        void shouldCreateRoleSuccessfully() {
            role.getViewPermissions().add(PermissionEntity.SETTINGS);
            subscriptionPlan.getFeatures().add(PlanFeatures.ROLE);
            when(userService.whoami(request)).thenReturn(user);
            when(roleService.create(any(Role.class))).thenReturn(role);

            Role result = roleController.create(role, request);

            assertNotNull(result);
            assertTrue(result.isPaid());
        }
    }

    @Nested
    @DisplayName("Patch Role Tests")
    class PatchRoleTests {

        private RolePatchDTO patchDTO;

        @BeforeEach
        void setup() {
            patchDTO = new RolePatchDTO();
        }

        @Test
        @DisplayName("Should throw not found when role does not exist")
        void shouldThrowNotFoundWhenRoleDoesNotExist() {
            when(userService.whoami(request)).thenReturn(user);
            when(roleService.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> roleController.patch(patchDTO, 1L, request));
        }

        @Test
        @DisplayName("Should throw forbidden if user has no permission")
        void shouldThrowForbiddenIfUserHasNoPermission() {
            when(userService.whoami(request)).thenReturn(user);
            when(roleService.findById(anyLong())).thenReturn(Optional.of(role));

            assertThrows(CustomException.class, () -> roleController.patch(patchDTO, 1L, request));
        }

        @Test
        @DisplayName("Should patch role successfully")
        void shouldPatchRoleSuccessfully() {
            role.getViewPermissions().add(PermissionEntity.SETTINGS);
            when(userService.whoami(request)).thenReturn(user);
            when(roleService.findById(anyLong())).thenReturn(Optional.of(role));
            when(roleService.update(anyLong(), any(RolePatchDTO.class))).thenReturn(role);

            Role result = roleController.patch(patchDTO, 1L, request);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Delete Role Tests")
    class DeleteRoleTests {

        @Test
        @DisplayName("Should throw not found on delete")
        void shouldThrowNotFoundOnDelete() {
            when(userService.whoami(request)).thenReturn(user);
            when(roleService.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> roleController.delete(1L, request));
        }

        @Test
        @DisplayName("Should throw forbidden if user has no permission")
        void shouldThrowForbiddenIfUserHasNoPermission() {
            when(userService.whoami(request)).thenReturn(user);
            when(roleService.findById(anyLong())).thenReturn(Optional.of(role));

            assertThrows(CustomException.class, () -> roleController.delete(1L, request));
        }

        @Test
        @DisplayName("Should delete role successfully")
        void shouldDeleteRoleSuccessfully() {
            role.getViewPermissions().add(PermissionEntity.SETTINGS);
            when(userService.whoami(request)).thenReturn(user);
            when(roleService.findById(anyLong())).thenReturn(Optional.of(role));

            ResponseEntity response = roleController.delete(1L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(roleService).delete(1L);
        }
    }
}
