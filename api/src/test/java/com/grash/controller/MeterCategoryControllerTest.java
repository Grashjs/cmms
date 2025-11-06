package com.grash.controller;

import com.grash.dto.CategoryPatchDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.service.MeterCategoryService;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeterCategoryControllerTest {

    @Mock
    private MeterCategoryService meterCategoryService;

    @Mock
    private UserService userService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private MeterCategoryController meterCategoryController;

    private OwnUser clientUser;
    private OwnUser adminUser;
    private MeterCategory meterCategory;

    @BeforeEach
    void setUp() {
        clientUser = new OwnUser();
        clientUser.setId(1L);
        Role clientRole = new Role();
        clientRole.setRoleType(RoleType.ROLE_CLIENT);
        clientUser.setRole(clientRole);
        Company company = new Company();
        company.setId(1L);
        clientUser.setCompany(company);

        adminUser = new OwnUser();
        adminUser.setId(2L);
        Role adminRole = new Role();
        adminRole.setRoleType(RoleType.ROLE_SUPER_ADMIN);
        adminUser.setRole(adminRole);

        meterCategory = new MeterCategory();
        meterCategory.setId(1L);
        meterCategory.setName("Test Category");
        meterCategory.setCreatedBy(clientUser.getId());
    }

    @Nested
    @DisplayName("getAll method")
    class GetAllTests {

        @Test
        @DisplayName("Should return all meter categories for admin user")
        void getAll_asAdmin_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(adminUser);
            when(meterCategoryService.getAll()).thenReturn(Arrays.asList(meterCategory));

            Collection<MeterCategory> result = meterCategoryController.getAll(request);

            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should return meter categories for client with view permission")
        void getAll_asClientWithPermission_shouldSucceed() {
            clientUser.getRole().setViewPermissions(new HashSet<>(Arrays.asList(PermissionEntity.CATEGORIES)));
            when(userService.whoami(request)).thenReturn(clientUser);
            when(meterCategoryService.findByCompany(anyLong())).thenReturn(Arrays.asList(meterCategory));

            Collection<MeterCategory> result = meterCategoryController.getAll(request);

            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should throw exception for client without view permission")
        void getAll_asClientWithoutPermission_shouldThrowException() {
            clientUser.getRole().setViewPermissions(Collections.emptySet());
            when(userService.whoami(request)).thenReturn(clientUser);

            CustomException exception = assertThrows(CustomException.class, () -> meterCategoryController.getAll(request));

            assertEquals("Access Denied", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("getById method")
    class GetByIdTests {

        @Test
        @DisplayName("Should return meter category for user with view permission")
        void getById_withPermission_shouldSucceed() {
            clientUser.getRole().setViewPermissions(new HashSet<>(Arrays.asList(PermissionEntity.CATEGORIES)));
            when(userService.whoami(request)).thenReturn(clientUser);
            when(meterCategoryService.findById(1L)).thenReturn(Optional.of(meterCategory));

            MeterCategory result = meterCategoryController.getById(1L, request);

            assertNotNull(result);
            assertEquals(meterCategory.getId(), result.getId());
        }

        @Test
        @DisplayName("Should throw exception when meter category not found")
        void getById_notFound_shouldThrowException() {
            clientUser.getRole().setViewPermissions(new HashSet<>(Arrays.asList(PermissionEntity.CATEGORIES)));
            when(userService.whoami(request)).thenReturn(clientUser);
            when(meterCategoryService.findById(1L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class, () -> meterCategoryController.getById(1L, request));

            assertEquals("Not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }

        @Test
        @DisplayName("Should throw exception for user without view permission")
        void getById_withoutPermission_shouldThrowException() {
            clientUser.getRole().setViewPermissions(Collections.emptySet());
            when(userService.whoami(request)).thenReturn(clientUser);

            CustomException exception = assertThrows(CustomException.class, () -> meterCategoryController.getById(1L, request));

            assertEquals("Access Denied", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("create method")
    class CreateTests {

        @Test
        @DisplayName("Should create meter category for user with create permission")
        void create_withPermission_shouldSucceed() {
            clientUser.getRole().setCreatePermissions(new HashSet<>(Arrays.asList(PermissionEntity.CATEGORIES)));
            when(userService.whoami(request)).thenReturn(clientUser);
            when(meterCategoryService.create(any(MeterCategory.class))).thenReturn(meterCategory);

            MeterCategory result = meterCategoryController.create(meterCategory, request);

            assertNotNull(result);
            assertEquals(meterCategory.getName(), result.getName());
        }

        @Test
        @DisplayName("Should throw exception for user without create permission")
        void create_withoutPermission_shouldThrowException() {
            clientUser.getRole().setCreatePermissions(Collections.emptySet());
            when(userService.whoami(request)).thenReturn(clientUser);

            CustomException exception = assertThrows(CustomException.class, () -> meterCategoryController.create(meterCategory, request));

            assertEquals("Access denied", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("patch method")
    class PatchTests {

        private CategoryPatchDTO patchDTO;

        @BeforeEach
        void patchSetup() {
            patchDTO = new CategoryPatchDTO();
            patchDTO.setName("Updated Name");
        }

        @Test
        @DisplayName("Should patch meter category for user with create permission")
        void patch_withPermission_shouldSucceed() {
            clientUser.getRole().setCreatePermissions(new HashSet<>(Arrays.asList(PermissionEntity.CATEGORIES)));
            when(userService.whoami(request)).thenReturn(clientUser);
            when(meterCategoryService.findById(1L)).thenReturn(Optional.of(meterCategory));
            when(meterCategoryService.update(anyLong(), any(CategoryPatchDTO.class))).thenReturn(meterCategory);

            MeterCategory result = meterCategoryController.patch(patchDTO, 1L, request);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should throw exception when meter category not found")
        void patch_notFound_shouldThrowException() {
            clientUser.getRole().setCreatePermissions(new HashSet<>(Arrays.asList(PermissionEntity.CATEGORIES)));
            when(userService.whoami(request)).thenReturn(clientUser);
            when(meterCategoryService.findById(1L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class, () -> meterCategoryController.patch(patchDTO, 1L, request));

            assertEquals("MeterCategory not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }

        @Test
        @DisplayName("Should throw exception for user without create permission")
        void patch_withoutPermission_shouldThrowException() {
            clientUser.getRole().setCreatePermissions(Collections.emptySet());
            when(userService.whoami(request)).thenReturn(clientUser);

            CustomException exception = assertThrows(CustomException.class, () -> meterCategoryController.patch(patchDTO, 1L, request));

            assertEquals("Access Denied", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("delete method")
    class DeleteTests {

        @Test
        @DisplayName("Should delete meter category if user is creator")
        void delete_asCreator_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(clientUser);
            when(meterCategoryService.findById(1L)).thenReturn(Optional.of(meterCategory));
            doNothing().when(meterCategoryService).delete(1L);

            ResponseEntity<SuccessResponse> response = meterCategoryController.delete(1L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().isSuccess());
            assertEquals("Deleted successfully", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Should delete meter category if user has delete other permission")
        void delete_withDeleteOtherPermission_shouldSucceed() {
            clientUser.getRole().setDeleteOtherPermissions(new HashSet<>(Arrays.asList(PermissionEntity.CATEGORIES)));
            meterCategory.setCreatedBy(99L); // Different creator
            when(userService.whoami(request)).thenReturn(clientUser);
            when(meterCategoryService.findById(1L)).thenReturn(Optional.of(meterCategory));
            doNothing().when(meterCategoryService).delete(1L);

            ResponseEntity<SuccessResponse> response = meterCategoryController.delete(1L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().isSuccess());
        }

        @Test
        @DisplayName("Should throw exception if user is not creator and no delete other permission")
        void delete_withoutPermission_shouldThrowException() {
            meterCategory.setCreatedBy(99L);
            clientUser.getRole().setDeleteOtherPermissions(Collections.emptySet());
            when(userService.whoami(request)).thenReturn(clientUser);
            when(meterCategoryService.findById(1L)).thenReturn(Optional.of(meterCategory));

            CustomException exception = assertThrows(CustomException.class, () -> meterCategoryController.delete(1L, request));

            assertEquals("Forbidden", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }

        @Test
        @DisplayName("Should throw exception when meter category not found")
        void delete_notFound_shouldThrowException() {
            when(userService.whoami(request)).thenReturn(clientUser);
            when(meterCategoryService.findById(1L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class, () -> meterCategoryController.delete(1L, request));

            assertEquals("MeterCategory not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }
    }
}
