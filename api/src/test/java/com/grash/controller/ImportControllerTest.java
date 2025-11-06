package com.grash.controller;

import com.grash.dto.imports.*;
import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.enums.ImportEntity;
import com.grash.model.enums.Language;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PlanFeatures;
import com.grash.model.enums.RoleType;
import com.grash.service.ImportService;
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

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private ImportService importService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private ImportController importController;

    private OwnUser clientUser;

    @BeforeEach
    void setUp() {
        clientUser = new OwnUser();
        clientUser.setId(1L);
        Role clientRole = new Role();
        clientRole.setRoleType(RoleType.ROLE_CLIENT);
        clientUser.setRole(clientRole);
        Company company = new Company();
        Subscription subscription = new Subscription();
        SubscriptionPlan subscriptionPlan = new SubscriptionPlan();
        subscriptionPlan.setFeatures(new HashSet<>(Arrays.asList(PlanFeatures.IMPORT_CSV)));
        subscription.setSubscriptionPlan(subscriptionPlan);
        company.setSubscription(subscription);
        clientUser.setCompany(company);
    }

    @Nested
    @DisplayName("importWorkOrders method")
    class ImportWorkOrdersTests {

        @Test
        @DisplayName("Should import work orders for user with permission and feature")
        void importWorkOrders_withPermissionAndFeature_shouldSucceed() {
            clientUser.getRole().setCreatePermissions(new HashSet<>(Arrays.asList(PermissionEntity.WORK_ORDERS)));
            when(userService.whoami(request)).thenReturn(clientUser);
            when(importService.importWorkOrders(any(), any(Company.class))).thenReturn(new ImportResponse());

            ImportResponse response = importController.importWorkOrders(Collections.singletonList(new WorkOrderImportDTO()), request);

            assertNotNull(response);
        }

        @Test
        @DisplayName("Should throw exception for user without permission")
        void importWorkOrders_withoutPermission_shouldThrowException() {
            clientUser.getRole().setCreatePermissions(Collections.emptySet());
            when(userService.whoami(request)).thenReturn(clientUser);

            CustomException exception = assertThrows(CustomException.class, () -> importController.importWorkOrders(Collections.singletonList(new WorkOrderImportDTO()), request));

            assertEquals("Access Denied", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("importAssets method")
    class ImportAssetsTests {

        @Test
        @DisplayName("Should import assets for user with permission and feature")
        void importAssets_withPermissionAndFeature_shouldSucceed() {
            clientUser.getRole().setCreatePermissions(new HashSet<>(Arrays.asList(PermissionEntity.ASSETS)));
            when(userService.whoami(request)).thenReturn(clientUser);
            when(importService.importAssets(any(), any(Company.class))).thenReturn(new ImportResponse());

            ImportResponse response = importController.importAssets(Collections.singletonList(new AssetImportDTO()), request);

            assertNotNull(response);
        }

        @Test
        @DisplayName("Should throw exception for user without permission")
        void importAssets_withoutPermission_shouldThrowException() {
            clientUser.getRole().setCreatePermissions(Collections.emptySet());
            when(userService.whoami(request)).thenReturn(clientUser);

            CustomException exception = assertThrows(CustomException.class, () -> importController.importAssets(Collections.singletonList(new AssetImportDTO()), request));

            assertEquals("Access Denied", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("importLocations method")
    class ImportLocationsTests {

        @Test
        @DisplayName("Should import locations for user with permission and feature")
        void importLocations_withPermissionAndFeature_shouldSucceed() {
            clientUser.getRole().setCreatePermissions(new HashSet<>(Arrays.asList(PermissionEntity.LOCATIONS)));
            when(userService.whoami(request)).thenReturn(clientUser);
            when(importService.importLocations(any(), any(Company.class))).thenReturn(new ImportResponse());

            ImportResponse response = importController.importLocations(Collections.singletonList(new LocationImportDTO()), request);

            assertNotNull(response);
        }

        @Test
        @DisplayName("Should throw exception for user without permission")
        void importLocations_withoutPermission_shouldThrowException() {
            clientUser.getRole().setCreatePermissions(Collections.emptySet());
            when(userService.whoami(request)).thenReturn(clientUser);

            CustomException exception = assertThrows(CustomException.class, () -> importController.importLocations(Collections.singletonList(new LocationImportDTO()), request));

            assertEquals("Access Denied", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("importMeters method")
    class ImportMetersTests {

        @Test
        @DisplayName("Should import meters for user with permission and feature")
        void importMeters_withPermissionAndFeature_shouldSucceed() {
            clientUser.getRole().setCreatePermissions(new HashSet<>(Arrays.asList(PermissionEntity.METERS)));
            when(userService.whoami(request)).thenReturn(clientUser);
            when(importService.importMeters(any(), any(Company.class))).thenReturn(new ImportResponse());

            ImportResponse response = importController.importMeters(Collections.singletonList(new MeterImportDTO()), request);

            assertNotNull(response);
        }

        @Test
        @DisplayName("Should throw exception for user without permission")
        void importMeters_withoutPermission_shouldThrowException() {
            clientUser.getRole().setCreatePermissions(Collections.emptySet());
            when(userService.whoami(request)).thenReturn(clientUser);

            CustomException exception = assertThrows(CustomException.class, () -> importController.importMeters(Collections.singletonList(new MeterImportDTO()), request));

            assertEquals("Access Denied", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("importParts method")
    class ImportPartsTests {

        @Test
        @DisplayName("Should import parts for user with permission and feature")
        void importParts_withPermissionAndFeature_shouldSucceed() {
            clientUser.getRole().setCreatePermissions(new HashSet<>(Arrays.asList(PermissionEntity.PARTS_AND_MULTIPARTS)));
            when(userService.whoami(request)).thenReturn(clientUser);
            when(importService.importParts(any(), any(Company.class))).thenReturn(new ImportResponse());

            ImportResponse response = importController.importParts(Collections.singletonList(new PartImportDTO()), request);

            assertNotNull(response);
        }

        @Test
        @DisplayName("Should throw exception for user without permission")
        void importParts_withoutPermission_shouldThrowException() {
            clientUser.getRole().setCreatePermissions(Collections.emptySet());
            when(userService.whoami(request)).thenReturn(clientUser);

            CustomException exception = assertThrows(CustomException.class, () -> importController.importParts(Collections.singletonList(new PartImportDTO()), request));

            assertEquals("Access Denied", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("downloadTemplate method")
    class DownloadTemplateTests {

        @Test
        @DisplayName("Should download template for given language and entity")
        void downloadTemplate_shouldSucceed() throws IOException {
            byte[] result = importController.importMeters(Language.EN, ImportEntity.ASSET, request);

            assertNotNull(result);
            assertTrue(result.length > 0);
        }

        @Test
        @DisplayName("Should download fallback template when language not found")
        void downloadTemplate_withFallback_shouldSucceed() throws IOException {
            // Assuming 'XX' is not a supported language, so it falls back to 'en'
            byte[] result = importController.importMeters(Language.FR, ImportEntity.ASSET, request);

            assertNotNull(result);
            assertTrue(result.length > 0);
        }
    }
}
