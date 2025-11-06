package com.grash.controller;

import com.grash.dto.CategoryPatchDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.AssetCategory;
import com.grash.model.Company;
import com.grash.model.CompanySettings;
import com.grash.model.OwnUser;
import com.grash.model.Role;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.service.AssetCategoryService;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetCategoryControllerTest {

    @Mock
    private AssetCategoryService assetCategoryService;

    @Mock
    private UserService userService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AssetCategoryController assetCategoryController;

    private OwnUser user;
    private AssetCategory assetCategory;

    @BeforeEach
    void setUp() {
        CompanySettings companySettings = new CompanySettings();
        companySettings.setId(1L);

        Company company = new Company();
        company.setCompanySettings(companySettings);

        Role role = new Role();
        role.setRoleType(RoleType.ROLE_CLIENT);
        role.setViewPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.CATEGORIES)));
        role.setCreatePermissions(new HashSet<>(Collections.singletonList(PermissionEntity.CATEGORIES)));
        role.setDeleteOtherPermissions(new HashSet<>());

        user = new OwnUser();
        user.setId(1L);
        user.setCompany(company);
        user.setRole(role);

        assetCategory = new AssetCategory();
        assetCategory.setId(1L);
        assetCategory.setCreatedBy(1L);
    }

    @Nested
    @DisplayName("getAll method")
    class GetAllTests {
        @Test
        @DisplayName("Should return categories for client with view permission")
        void getAll_clientWithPermission_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(assetCategoryService.findByCompanySettings(1L)).thenReturn(Collections.singletonList(assetCategory));

            Collection<AssetCategory> result = assetCategoryController.getAll(request);

            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("Should throw exception for client without view permission")
        void getAll_clientWithoutPermission_shouldThrowException() {
            user.getRole().setViewPermissions(new HashSet<>());
            when(userService.whoami(request)).thenReturn(user);

            assertThrows(CustomException.class, () -> assetCategoryController.getAll(request));
        }

        @Test
        @DisplayName("Should return all categories for non-client user")
        void getAll_nonClient_shouldSucceed() {
            user.getRole().setRoleType(RoleType.ROLE_SUPER_ADMIN);
            when(userService.whoami(request)).thenReturn(user);
            when(assetCategoryService.getAll()).thenReturn(Collections.singletonList(assetCategory));

            Collection<AssetCategory> result = assetCategoryController.getAll(request);

            assertFalse(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getById method")
    class GetByIdTests {
        @Test
        @DisplayName("Should return category when found and user has permission")
        void getById_foundAndPermitted_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(assetCategoryService.findById(1L)).thenReturn(Optional.of(assetCategory));

            AssetCategory result = assetCategoryController.getById(1L, request);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should throw exception when user has no permission")
        void getById_noPermission_shouldThrowException() {
            user.getRole().setViewPermissions(new HashSet<>());
            when(userService.whoami(request)).thenReturn(user);

            assertThrows(CustomException.class, () -> assetCategoryController.getById(1L, request));
        }
    }

    @Nested
    @DisplayName("create method")
    class CreateTests {
        @Test
        @DisplayName("Should create category when user has permission")
        void create_withPermission_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(assetCategoryService.create(any(AssetCategory.class))).thenReturn(assetCategory);

            AssetCategory result = assetCategoryController.create(new AssetCategory(), request);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should throw exception when user has no permission")
        void create_withoutPermission_shouldThrowException() {
            user.getRole().setCreatePermissions(new HashSet<>());
            when(userService.whoami(request)).thenReturn(user);

            assertThrows(CustomException.class, () -> assetCategoryController.create(new AssetCategory(), request));
        }
    }

    @Nested
    @DisplayName("patch method")
    class PatchTests {
        @Test
        @DisplayName("Should patch category when user has permission")
        void patch_withPermission_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(assetCategoryService.findById(1L)).thenReturn(Optional.of(assetCategory));
            when(assetCategoryService.update(any(Long.class), any(CategoryPatchDTO.class))).thenReturn(assetCategory);

            AssetCategory result = assetCategoryController.patch(new CategoryPatchDTO(), 1L, request);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should throw exception when category not found")
        void patch_notFound_shouldThrowException() {
            when(userService.whoami(request)).thenReturn(user);
            when(assetCategoryService.findById(1L)).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> assetCategoryController.patch(new CategoryPatchDTO(), 1L, request));
        }
    }

    @Nested
    @DisplayName("delete method")
    class DeleteTests {
        @Test
        @DisplayName("Should delete category when user is creator")
        void delete_asCreator_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(assetCategoryService.findById(1L)).thenReturn(Optional.of(assetCategory));

            ResponseEntity<SuccessResponse> response = assetCategoryController.delete(1L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().isSuccess());
        }

        @Test
        @DisplayName("Should throw exception when user is not creator and has no delete-other permission")
        void delete_notCreatorAndNoPermission_shouldThrowException() {
            assetCategory.setCreatedBy(2L);
            when(userService.whoami(request)).thenReturn(user);
            when(assetCategoryService.findById(1L)).thenReturn(Optional.of(assetCategory));

            assertThrows(CustomException.class, () -> assetCategoryController.delete(1L, request));
        }
    }
}
