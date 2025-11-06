package com.grash.controller;

import com.grash.dto.AssetDowntimePatchDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.Asset;
import com.grash.model.AssetDowntime;
import com.grash.model.OwnUser;
import com.grash.model.Role;
import com.grash.model.enums.PermissionEntity;
import com.grash.service.AssetDowntimeService;
import com.grash.service.AssetService;
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
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetDowntimeControllerTest {

    @Mock
    private AssetDowntimeService assetDowntimeService;

    @Mock
    private UserService userService;

    @Mock
    private AssetService assetService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AssetDowntimeController assetDowntimeController;

    private OwnUser user;
    private Asset asset;
    private AssetDowntime assetDowntime;

    @BeforeEach
    void setUp() {
        Role role = new Role();
        role.setViewPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.ASSETS)));
        role.setCreatePermissions(new HashSet<>(Collections.singletonList(PermissionEntity.CATEGORIES)));
        role.setEditOtherPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.ASSETS)));

        user = new OwnUser();
        user.setId(1L);
        user.setRole(role);

        asset = new Asset();
        asset.setId(1L);
        asset.setCreatedBy(1L);
        asset.setInServiceDate(new Date(System.currentTimeMillis() - 100000));

        assetDowntime = new AssetDowntime();
        assetDowntime.setId(1L);
        assetDowntime.setAsset(asset);
        assetDowntime.setStartsOn(new Date());
    }

    @Nested
    @DisplayName("getById method")
    class GetByIdTests {
        @Test
        @DisplayName("Should return downtime when found and user has permission")
        void getById_foundAndPermitted_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(assetDowntimeService.findById(1L)).thenReturn(Optional.of(assetDowntime));

            AssetDowntime result = assetDowntimeController.getById(1L, request);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should throw exception when user has no permission")
        void getById_noPermission_shouldThrowException() {
            user.getRole().setViewPermissions(new HashSet<>());
            when(userService.whoami(request)).thenReturn(user);

            assertThrows(CustomException.class, () -> assetDowntimeController.getById(1L, request));
        }
    }

    @Nested
    @DisplayName("create method")
    class CreateTests {
        @Test
        @DisplayName("Should create downtime when user has permission")
        void create_withPermission_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(assetService.findById(1L)).thenReturn(Optional.of(asset));
            when(assetDowntimeService.create(any(AssetDowntime.class))).thenReturn(assetDowntime);

            AssetDowntime result = assetDowntimeController.create(assetDowntime, request);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should throw exception when asset not found")
        void create_assetNotFound_shouldThrowException() {
            when(userService.whoami(request)).thenReturn(user);
            when(assetService.findById(1L)).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> assetDowntimeController.create(assetDowntime, request));
        }

        @Test
        @DisplayName("Should throw exception when downtime starts before in-service date")
        void create_startsOnBeforeInServiceDate_shouldThrowException() {
            assetDowntime.setStartsOn(new Date(System.currentTimeMillis() - 200000));
            when(userService.whoami(request)).thenReturn(user);
            when(assetService.findById(1L)).thenReturn(Optional.of(asset));

            assertThrows(CustomException.class, () -> assetDowntimeController.create(assetDowntime, request));
        }
    }

    @Nested
    @DisplayName("getByAsset method")
    class GetByAssetTests {
        @Test
        @DisplayName("Should return downtimes when asset found")
        void getByAsset_found_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(assetService.findById(1L)).thenReturn(Optional.of(asset));
            when(assetDowntimeService.findByAsset(1L)).thenReturn(Collections.singletonList(assetDowntime));

            Collection<AssetDowntime> result = assetDowntimeController.getByAsset(1L, request);

            assertFalse(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("patch method")
    class PatchTests {
        @Test
        @DisplayName("Should patch downtime when user has permission")
        void patch_withPermission_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(assetDowntimeService.findById(1L)).thenReturn(Optional.of(assetDowntime));
            when(assetDowntimeService.update(any(Long.class), any(AssetDowntimePatchDTO.class))).thenReturn(assetDowntime);

            AssetDowntime result = assetDowntimeController.patch(new AssetDowntimePatchDTO(), 1L, request);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should throw exception when downtime not found")
        void patch_notFound_shouldThrowException() {
            when(userService.whoami(request)).thenReturn(user);
            when(assetDowntimeService.findById(1L)).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> assetDowntimeController.patch(new AssetDowntimePatchDTO(), 1L, request));
        }
    }

    @Nested
    @DisplayName("delete method")
    class DeleteTests {
        @Test
        @DisplayName("Should delete downtime when user has permission")
        void delete_withPermission_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(assetDowntimeService.findById(1L)).thenReturn(Optional.of(assetDowntime));

            ResponseEntity<SuccessResponse> response = assetDowntimeController.delete(1L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().isSuccess());
        }

        @Test
        @DisplayName("Should throw exception when downtime not found")
        void delete_notFound_shouldThrowException() {
            when(userService.whoami(request)).thenReturn(user);
            when(assetDowntimeService.findById(1L)).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> assetDowntimeController.delete(1L, request));
        }
    }
}
