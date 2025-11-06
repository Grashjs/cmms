package com.grash.controller;

import com.grash.dto.MultiPartsMiniDTO;
import com.grash.dto.MultiPartsPatchDTO;
import com.grash.dto.MultiPartsShowDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.mapper.MultiPartsMapper;
import com.grash.model.Company;
import com.grash.model.MultiParts;
import com.grash.model.OwnUser;
import com.grash.model.Role;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.service.MultiPartsService;
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
class MultiPartsControllerTest {

    @Mock
    private MultiPartsService multiPartsService;

    @Mock
    private MultiPartsMapper multiPartsMapper;

    @Mock
    private UserService userService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private MultiPartsController multiPartsController;

    private OwnUser user;
    private MultiParts multiParts;
    private MultiPartsShowDTO multiPartsShowDTO;
    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);

        Role role = new Role();
        role.setRoleType(RoleType.ROLE_CLIENT);
        role.setViewPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.PARTS_AND_MULTIPARTS)));
        role.setCreatePermissions(new HashSet<>(Collections.singletonList(PermissionEntity.PARTS_AND_MULTIPARTS)));
        role.setEditOtherPermissions(new HashSet<>());
        role.setDeleteOtherPermissions(new HashSet<>());
        role.setViewOtherPermissions(new HashSet<>());

        user = new OwnUser();
        user.setId(1L);
        user.setCompany(company);
        user.setRole(role);

        multiParts = new MultiParts();
        multiParts.setId(1L);
        multiParts.setCreatedBy(1L);

        multiPartsShowDTO = new MultiPartsShowDTO();
        multiPartsShowDTO.setId(1L);
    }

    @Nested
    @DisplayName("getAll method")
    class GetAllTests {
        @Test
        @DisplayName("Should return DTOs for client with view permission")
        void getAll_clientWithPermission_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(multiPartsService.findByCompany(1L)).thenReturn(Collections.singletonList(multiParts));
            when(multiPartsMapper.toShowDto(any(MultiParts.class))).thenReturn(multiPartsShowDTO);

            Collection<MultiPartsShowDTO> result = multiPartsController.getAll(request);

            assertFalse(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getById method")
    class GetByIdTests {
        @Test
        @DisplayName("Should return DTO when found and user has permission")
        void getById_foundAndPermitted_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(multiPartsService.findById(1L)).thenReturn(Optional.of(multiParts));
            when(multiPartsMapper.toShowDto(any(MultiParts.class))).thenReturn(multiPartsShowDTO);

            MultiPartsShowDTO result = multiPartsController.getById(1L, request);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should throw exception when user has no permission")
        void getById_noPermission_shouldThrowException() {
            user.getRole().setViewPermissions(new HashSet<>());
            when(userService.whoami(request)).thenReturn(user);
            when(multiPartsService.findById(1L)).thenReturn(Optional.of(multiParts));

            assertThrows(CustomException.class, () -> multiPartsController.getById(1L, request));
        }
    }

    @Nested
    @DisplayName("create method")
    class CreateTests {
        @Test
        @DisplayName("Should create and return DTO when user has permission")
        void create_withPermission_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(multiPartsService.create(any(MultiParts.class))).thenReturn(multiParts);
            when(multiPartsMapper.toShowDto(any(MultiParts.class))).thenReturn(multiPartsShowDTO);

            MultiPartsShowDTO result = multiPartsController.create(new MultiParts(), request);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("patch method")
    class PatchTests {
        @Test
        @DisplayName("Should patch and return DTO when user has permission")
        void patch_withPermission_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(multiPartsService.findById(1L)).thenReturn(Optional.of(multiParts));
            when(multiPartsService.update(any(Long.class), any(MultiPartsPatchDTO.class))).thenReturn(multiParts);
            when(multiPartsMapper.toShowDto(any(MultiParts.class))).thenReturn(multiPartsShowDTO);

            MultiPartsShowDTO result = multiPartsController.patch(new MultiPartsPatchDTO(), 1L, request);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("getMini method")
    class GetMiniTests {
        @Test
        @DisplayName("Should return a collection of mini DTOs")
        void getMini_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(user);
            when(multiPartsService.findByCompany(company.getId())).thenReturn(Collections.singletonList(multiParts));
            when(multiPartsMapper.toMiniDto(multiParts)).thenReturn(new MultiPartsMiniDTO());

            Collection<MultiPartsMiniDTO> result = multiPartsController.getMini(request);

            assertFalse(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("delete method")
    class DeleteTests {
        @Test
        @DisplayName("Should delete when user is creator")
        void delete_asCreator_shouldSucceed() {
            multiParts.setId(user.getId()); // Match creator id with user id
            when(userService.whoami(request)).thenReturn(user);
            when(multiPartsService.findById(1L)).thenReturn(Optional.of(multiParts));

            ResponseEntity<SuccessResponse> response = multiPartsController.delete(1L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().isSuccess());
        }

        @Test
        @DisplayName("Should throw exception when user is not authorized")
        void delete_notAuthorized_shouldThrowException() {
            multiParts.setId(2L); // Different id
            when(userService.whoami(request)).thenReturn(user);
            when(multiPartsService.findById(1L)).thenReturn(Optional.of(multiParts));

            assertThrows(CustomException.class, () -> multiPartsController.delete(1L, request));
        }
    }
}
