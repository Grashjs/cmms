package com.grash.controller;

import com.grash.dto.CategoryPatchDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.Company;
import com.grash.model.CompanySettings;
import com.grash.model.OwnUser;
import com.grash.model.Role;
import com.grash.model.TimeCategory;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.service.TimeCategoryService;
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
class TimeCategoryControllerTest {

    @Mock
    private TimeCategoryService timeCategoryService;

    @Mock
    private UserService userService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private TimeCategoryController timeCategoryController;

    private OwnUser user;
    private TimeCategory timeCategory;
    private Company company;
    private CompanySettings companySettings;

    @BeforeEach
    void setUp() {
        companySettings = new CompanySettings();
        companySettings.setId(1L);

        company = new Company();
        company.setId(1L);
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

        timeCategory = new TimeCategory();
        timeCategory.setId(1L);
        timeCategory.setCreatedBy(1L);
    }

    @Nested
    @DisplayName("getAll method")
    class GetAllTests {
        @Test
        @DisplayName("Should return categories for client with view permission")
        void getAll_clientWithPermission_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(timeCategoryService.findByCompanySettings(1L)).thenReturn(Collections.singletonList(timeCategory));

            Collection<TimeCategory> result = timeCategoryController.getAll(request);

            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("Should throw exception for client without view permission")
        void getAll_clientWithoutPermission_shouldThrowException() {
            user.getRole().getViewPermissions().clear();
            when(userService.whoami(request)).thenReturn(user);

            assertThrows(CustomException.class, () -> timeCategoryController.getAll(request));
        }
    }

    @Nested
    @DisplayName("getById method")
    class GetByIdTests {
        @Test
        @DisplayName("Should return category when found and user has permission")
        void getById_foundAndPermitted_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(timeCategoryService.findById(1L)).thenReturn(Optional.of(timeCategory));

            TimeCategory result = timeCategoryController.getById(1L, request);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should throw exception when user has no permission")
        void getById_noPermission_shouldThrowException() {
            user.getRole().getViewPermissions().clear();
            when(userService.whoami(request)).thenReturn(user);

            assertThrows(CustomException.class, () -> timeCategoryController.getById(1L, request));
        }
    }

    @Nested
    @DisplayName("create method")
    class CreateTests {
        @Test
        @DisplayName("Should create category when user has permission")
        void create_withPermission_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(timeCategoryService.create(any(TimeCategory.class))).thenReturn(timeCategory);

            TimeCategory result = timeCategoryController.create(new TimeCategory(), request);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("patch method")
    class PatchTests {
        @Test
        @DisplayName("Should patch category when user has permission")
        void patch_withPermission_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(timeCategoryService.findById(1L)).thenReturn(Optional.of(timeCategory));
            when(timeCategoryService.update(any(Long.class), any(CategoryPatchDTO.class))).thenReturn(timeCategory);

            TimeCategory result = timeCategoryController.patch(new CategoryPatchDTO(), 1L, request);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should throw exception when category not found")
        void patch_notFound_shouldThrowException() {
            when(userService.whoami(request)).thenReturn(user);
            when(timeCategoryService.findById(1L)).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> timeCategoryController.patch(new CategoryPatchDTO(), 1L, request));
        }
    }

    @Nested
    @DisplayName("delete method")
    class DeleteTests {
        @Test
        @DisplayName("Should delete category when user is creator")
        void delete_asCreator_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(timeCategoryService.findById(1L)).thenReturn(Optional.of(timeCategory));

            ResponseEntity<SuccessResponse> response = timeCategoryController.delete(1L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().isSuccess());
        }

        @Test
        @DisplayName("Should throw exception when user is not authorized")
        void delete_notAuthorized_shouldThrowException() {
            timeCategory.setCreatedBy(2L); // Different creator
            when(userService.whoami(request)).thenReturn(user);
            when(timeCategoryService.findById(1L)).thenReturn(Optional.of(timeCategory));

            assertThrows(CustomException.class, () -> timeCategoryController.delete(1L, request));
        }
    }
}
