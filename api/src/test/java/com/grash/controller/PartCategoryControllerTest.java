package com.grash.controller;

import com.grash.dto.CategoryPatchDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.Company;
import com.grash.model.CompanySettings;
import com.grash.model.PartCategory;
import com.grash.model.OwnUser;
import com.grash.model.Role;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.service.PartCategoryService;
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
class PartCategoryControllerTest {

    @Mock
    private PartCategoryService partCategoryService;

    @Mock
    private UserService userService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private PartCategoryController partCategoryController;

    private OwnUser clientUser;
    private OwnUser adminUser;
    private PartCategory partCategory;

    @BeforeEach
    void setUp() {
        clientUser = new OwnUser();
        clientUser.setId(1L);
        Role clientRole = new Role();
        clientRole.setRoleType(RoleType.ROLE_CLIENT);
        clientUser.setRole(clientRole);
        Company company = new Company();
        CompanySettings companySettings = new CompanySettings();
        companySettings.setId(1L);
        company.setCompanySettings(companySettings);
        clientUser.setCompany(company);

        adminUser = new OwnUser();
        adminUser.setId(2L);
        Role adminRole = new Role();
        adminRole.setRoleType(RoleType.ROLE_SUPER_ADMIN);
        adminUser.setRole(adminRole);

        partCategory = new PartCategory();
        partCategory.setId(1L);
        partCategory.setName("Test Category");
        partCategory.setCreatedBy(clientUser.getId());
    }

    @Nested
    @DisplayName("getAll method")
    class GetAllTests {

        @Test
        @DisplayName("Should return all part categories for admin user")
        void getAll_asAdmin_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(adminUser);
            when(partCategoryService.getAll()).thenReturn(Arrays.asList(partCategory));

            Collection<PartCategory> result = partCategoryController.getAll(request);

            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should return part categories for client with view permission")
        void getAll_asClientWithPermission_shouldSucceed() {
            clientUser.getRole().setViewPermissions(new HashSet<>(Arrays.asList(PermissionEntity.CATEGORIES)));
            when(userService.whoami(request)).thenReturn(clientUser);
            when(partCategoryService.findByCompanySettings(anyLong())).thenReturn(Arrays.asList(partCategory));

            Collection<PartCategory> result = partCategoryController.getAll(request);

            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should throw exception for client without view permission")
        void getAll_asClientWithoutPermission_shouldThrowException() {
            clientUser.getRole().setViewPermissions(Collections.emptySet());
            when(userService.whoami(request)).thenReturn(clientUser);

            CustomException exception = assertThrows(CustomException.class, () -> partCategoryController.getAll(request));

            assertEquals("Access Denied", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("getById method")
    class GetByIdTests {

        @Test
        @DisplayName("Should return part category for user with view permission")
        void getById_withPermission_shouldSucceed() {
            clientUser.getRole().setViewPermissions(new HashSet<>(Arrays.asList(PermissionEntity.CATEGORIES)));
            when(userService.whoami(request)).thenReturn(clientUser);
            when(partCategoryService.findById(1L)).thenReturn(Optional.of(partCategory));

            PartCategory result = partCategoryController.getById(1L, request);

            assertNotNull(result);
            assertEquals(partCategory.getId(), result.getId());
        }

        @Test
        @DisplayName("Should throw exception when part category not found")
        void getById_notFound_shouldThrowException() {
            clientUser.getRole().setViewPermissions(new HashSet<>(Arrays.asList(PermissionEntity.CATEGORIES)));
            when(userService.whoami(request)).thenReturn(clientUser);
            when(partCategoryService.findById(1L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class, () -> partCategoryController.getById(1L, request));

            assertEquals("Not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }

        @Test
        @DisplayName("Should throw exception for user without view permission")
        void getById_withoutPermission_shouldThrowException() {
            clientUser.getRole().setViewPermissions(Collections.emptySet());
            when(userService.whoami(request)).thenReturn(clientUser);

            CustomException exception = assertThrows(CustomException.class, () -> partCategoryController.getById(1L, request));

            assertEquals("Access Denied", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("create method")
    class CreateTests {

        @Test
        @DisplayName("Should create part category for user with create permission")
        void create_withPermission_shouldSucceed() {
            clientUser.getRole().setCreatePermissions(new HashSet<>(Arrays.asList(PermissionEntity.CATEGORIES)));
            when(userService.whoami(request)).thenReturn(clientUser);
            when(partCategoryService.create(any(PartCategory.class))).thenReturn(partCategory);

            PartCategory result = partCategoryController.create(partCategory, request);

            assertNotNull(result);
            assertEquals(partCategory.getName(), result.getName());
        }

        @Test
        @DisplayName("Should throw exception for user without create permission")
        void create_withoutPermission_shouldThrowException() {
            clientUser.getRole().setCreatePermissions(Collections.emptySet());
            when(userService.whoami(request)).thenReturn(clientUser);

            CustomException exception = assertThrows(CustomException.class, () -> partCategoryController.create(partCategory, request));

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
        @DisplayName("Should patch part category for user with create permission")
        void patch_withPermission_shouldSucceed() {
            clientUser.getRole().setCreatePermissions(new HashSet<>(Arrays.asList(PermissionEntity.CATEGORIES)));
            when(userService.whoami(request)).thenReturn(clientUser);
            when(partCategoryService.findById(1L)).thenReturn(Optional.of(partCategory));
            when(partCategoryService.update(anyLong(), any(CategoryPatchDTO.class))).thenReturn(partCategory);

            PartCategory result = partCategoryController.patch(patchDTO, 1L, request);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should throw exception when part category not found")
        void patch_notFound_shouldThrowException() {
            clientUser.getRole().setCreatePermissions(new HashSet<>(Arrays.asList(PermissionEntity.CATEGORIES)));
            when(userService.whoami(request)).thenReturn(clientUser);
            when(partCategoryService.findById(1L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class, () -> partCategoryController.patch(patchDTO, 1L, request));

            assertEquals("Category not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }

        @Test
        @DisplayName("Should throw exception for user without create permission")
        void patch_withoutPermission_shouldThrowException() {
            clientUser.getRole().setCreatePermissions(Collections.emptySet());
            when(userService.whoami(request)).thenReturn(clientUser);
            // No need to mock findById for this test as it's short-circuited

            CustomException exception = assertThrows(CustomException.class, () -> partCategoryController.patch(patchDTO, 1L, request));

            assertEquals("Access Denied", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("delete method")
    class DeleteTests {

        @Test
        @DisplayName("Should delete part category if user is creator")
        void delete_asCreator_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(clientUser);
            when(partCategoryService.findById(1L)).thenReturn(Optional.of(partCategory));
            doNothing().when(partCategoryService).delete(1L);

            ResponseEntity<SuccessResponse> response = partCategoryController.delete(1L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().isSuccess());
            assertEquals("Deleted successfully", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Should delete part category if user has delete other permission")
        void delete_withDeleteOtherPermission_shouldSucceed() {
            clientUser.getRole().setDeleteOtherPermissions(new HashSet<>(Arrays.asList(PermissionEntity.CATEGORIES)));
            partCategory.setCreatedBy(99L); // Different creator
            when(userService.whoami(request)).thenReturn(clientUser);
            when(partCategoryService.findById(1L)).thenReturn(Optional.of(partCategory));
            doNothing().when(partCategoryService).delete(1L);

            ResponseEntity<SuccessResponse> response = partCategoryController.delete(1L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().isSuccess());
        }

        @Test
        @DisplayName("Should throw exception if user is not creator and no delete other permission")
        void delete_withoutPermission_shouldThrowException() {
            partCategory.setCreatedBy(99L);
            clientUser.getRole().setDeleteOtherPermissions(Collections.emptySet());
            when(userService.whoami(request)).thenReturn(clientUser);
            when(partCategoryService.findById(1L)).thenReturn(Optional.of(partCategory));

            CustomException exception = assertThrows(CustomException.class, () -> partCategoryController.delete(1L, request));

            assertEquals("Forbidden", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }

        @Test
        @DisplayName("Should throw exception when part category not found")
        void delete_notFound_shouldThrowException() {
            when(userService.whoami(request)).thenReturn(clientUser);
            when(partCategoryService.findById(1L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class, () -> partCategoryController.delete(1L, request));

            assertEquals("PartCategory not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }
    }
}