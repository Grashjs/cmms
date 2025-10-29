
package com.grash.controller;

import com.grash.mapper.AssetMapper;
import com.grash.model.enums.AssetStatus;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.grash.dto.AssetPatchDTO;
import com.grash.exception.CustomException;
import com.grash.dto.SuccessResponse;
import org.junit.jupiter.api.function.Executable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.grash.model.Company;
import com.grash.dto.AssetShowDTO;
import com.grash.model.Asset;
import com.grash.model.OwnUser;
import com.grash.model.Role;
import com.grash.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import javax.persistence.EntityManager;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetControllerTest {

    @Mock
    private AssetService assetService;
    @Mock
    private AssetMapper assetMapper;
    @Mock
    private UserService userService;
    @Mock
    private LocationService locationService;
    @Mock
    private PartService partService;
    @Mock
    private MessageSource messageSource;
    @Mock
    private EntityManager em;

    @InjectMocks
    private AssetController assetController;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testControllerInstantiation() {
        assertNotNull(assetController);
    }

    @Test
    void testGetById() {
        Asset asset = new Asset();
        asset.setId(1L);
        asset.setCreatedBy(1L);

        AssetShowDTO assetShowDTO = new AssetShowDTO();
        assetShowDTO.setId(1L);

        OwnUser user = new OwnUser();
        user.setId(1L);
        Role role = new Role();
        role.setViewPermissions(Collections.singleton(com.grash.model.enums.PermissionEntity.ASSETS));
        role.setViewOtherPermissions(Collections.singleton(com.grash.model.enums.PermissionEntity.ASSETS));
        user.setRole(role);

        HttpServletRequest req = mock(HttpServletRequest.class);

        when(userService.whoami(req)).thenReturn(user);
        when(assetService.findById(1L)).thenReturn(Optional.of(asset));
        when(assetMapper.toShowDto(asset, assetService)).thenReturn(assetShowDTO);

        AssetShowDTO result = assetController.getById(1L, req);

        assertEquals(1L, result.getId());
        verify(assetService, times(1)).findById(1L);
        verify(assetMapper, times(1)).toShowDto(asset, assetService);
    }

    @Test
    void testCreate() {
        Asset asset = new Asset();
        asset.setId(1L);
        AssetShowDTO assetShowDTO = new AssetShowDTO();
        assetShowDTO.setId(1L);

        OwnUser user = new OwnUser();
        user.setId(1L);
        Role role = new Role();
        role.setCreatePermissions(Collections.singleton(com.grash.model.enums.PermissionEntity.ASSETS));
        user.setRole(role);
        user.setCompany(new Company());

        HttpServletRequest req = mock(HttpServletRequest.class);

        when(userService.whoami(req)).thenReturn(user);
        when(assetService.create(asset, user)).thenReturn(asset);
        when(assetMapper.toShowDto(asset, assetService)).thenReturn(assetShowDTO);
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("message");

        AssetShowDTO result = assetController.create(asset, req);

        assertEquals(1L, result.getId());
        verify(assetService, times(1)).create(asset, user);
        verify(assetMapper, times(1)).toShowDto(asset, assetService);
    }

    @Test
    void testDelete() {
        Asset asset = new Asset();
        asset.setId(1L);
        asset.setCreatedBy(1L);

        OwnUser user = new OwnUser();
        user.setId(1L);
        Role role = new Role();
        role.setDeleteOtherPermissions(Collections.singleton(com.grash.model.enums.PermissionEntity.ASSETS));
        user.setRole(role);

        HttpServletRequest req = mock(HttpServletRequest.class);

        when(userService.whoami(req)).thenReturn(user);
        when(assetService.findById(1L)).thenReturn(Optional.of(asset));

        ResponseEntity<SuccessResponse> response = assetController.delete(1L, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(true, response.getBody().isSuccess());
        verify(assetService, times(1)).delete(1L);
    }

    @Test
    void testPatch() {
        Asset asset = new Asset();
        asset.setId(1L);
        AssetPatchDTO assetPatchDTO = new AssetPatchDTO();
        assetPatchDTO.setStatus(AssetStatus.OPERATIONAL);

        AssetShowDTO assetShowDTO = new AssetShowDTO();
        assetShowDTO.setId(1L);

        OwnUser user = new OwnUser();
        user.setId(1L);
        Role role = new Role();
        role.setEditOtherPermissions(Collections.singleton(com.grash.model.enums.PermissionEntity.ASSETS));
        user.setRole(role);
        user.setCompany(new Company());

        HttpServletRequest req = mock(HttpServletRequest.class);

        when(userService.whoami(req)).thenReturn(user);
        when(assetService.findById(1L)).thenReturn(Optional.of(asset));
        when(assetService.update(1L, assetPatchDTO)).thenReturn(asset);
        when(assetMapper.toShowDto(asset, assetService)).thenReturn(assetShowDTO);

        AssetShowDTO result = assetController.patch(assetPatchDTO, 1L, req);

        assertEquals(1L, result.getId());
        verify(assetService, times(1)).update(1L, assetPatchDTO);
        verify(assetMapper, times(1)).toShowDto(asset, assetService);
    }

    @Test
    void testPatchForbidden() {
        Asset asset = new Asset();
        asset.setId(1L);
        asset.setCreatedBy(2L);

        AssetPatchDTO assetPatchDTO = new AssetPatchDTO();

        OwnUser user = new OwnUser();
        user.setId(1L);
        Role role = new Role();
        role.setEditOtherPermissions(Collections.emptySet());
        user.setRole(role);

        HttpServletRequest req = mock(HttpServletRequest.class);

        when(userService.whoami(req)).thenReturn(user);
        when(assetService.findById(1L)).thenReturn(Optional.of(asset));

        CustomException exception = assertThrows(CustomException.class, () -> assetController.patch(assetPatchDTO, 1L, req));

        assertEquals("Forbidden", exception.getMessage());
        assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
    }
}
