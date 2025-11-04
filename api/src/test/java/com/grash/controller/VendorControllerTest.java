package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.VendorMiniDTO;
import com.grash.dto.VendorPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.VendorMapper;
import com.grash.model.Company;
import com.grash.model.OwnUser;
import com.grash.model.Role;
import com.grash.model.Vendor;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.service.UserService;
import com.grash.service.VendorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
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
class VendorControllerTest {

    @Mock
    private VendorService vendorService;
    @Mock
    private UserService userService;
    @Mock
    private VendorMapper vendorMapper;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private VendorController vendorController;

    private OwnUser user;
    private Vendor vendor;
    private Role clientRole;

    @BeforeEach
    void setUp() {
        Company company = new Company();
        company.setId(1L);

        clientRole = new Role();
        clientRole.setRoleType(RoleType.ROLE_CLIENT);

        user = new OwnUser();
        user.setId(1L);
        user.setCompany(company);
        user.setRole(clientRole);

        vendor = new Vendor();
        vendor.setId(1L);
        vendor.setCreatedBy(1L);
    }

    @Nested
    @DisplayName("Search Tests")
    class SearchTests {

        private SearchCriteria searchCriteria;

        @BeforeEach
        void setup() {
            searchCriteria = new SearchCriteria();
        }

        @Test
        @DisplayName("Should throw forbidden if client has no permission")
        void shouldThrowForbiddenIfClientHasNoPermission() {
            clientRole.setViewPermissions(new HashSet<>());
            when(userService.whoami(request)).thenReturn(user);

            assertThrows(CustomException.class, () -> vendorController.search(searchCriteria, request));
        }

        @Test
        @DisplayName("Should return vendors for non-client user")
        void shouldReturnVendorsForNonClientUser() {
            clientRole.setRoleType(RoleType.ROLE_ADMIN);
            Page<Vendor> page = new PageImpl<>(Collections.singletonList(vendor));
            when(userService.whoami(request)).thenReturn(user);
            when(vendorService.findBySearchCriteria(any(SearchCriteria.class))).thenReturn(page);

            ResponseEntity<Page<Vendor>> response = vendorController.search(searchCriteria, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertFalse(response.getBody().isEmpty());
        }

        @Test
        @DisplayName("Should return vendors for client with permission")
        void shouldReturnVendorsForClientWithPermission() {
            clientRole.setViewPermissions(Collections.singleton(PermissionEntity.VENDORS_AND_CUSTOMERS));
            Page<Vendor> page = new PageImpl<>(Collections.singletonList(vendor));
            when(userService.whoami(request)).thenReturn(user);
            when(vendorService.findBySearchCriteria(any(SearchCriteria.class))).thenReturn(page);

            ResponseEntity<Page<Vendor>> response = vendorController.search(searchCriteria, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertFalse(response.getBody().isEmpty());
        }
    }

    @Nested
    @DisplayName("Get By Id Tests")
    class GetByIdTests {

        @Test
        @DisplayName("Should throw not found when vendor does not exist")
        void shouldThrowNotFoundWhenVendorDoesNotExist() {
            when(userService.whoami(request)).thenReturn(user);
            when(vendorService.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> vendorController.getById(1L, request));
        }

        @Test
        @DisplayName("Should throw forbidden if user has no permission")
        void shouldThrowForbiddenIfUserHasNoPermission() {
            clientRole.setViewPermissions(new HashSet<>());
            when(userService.whoami(request)).thenReturn(user);
            when(vendorService.findById(anyLong())).thenReturn(Optional.of(vendor));

            assertThrows(CustomException.class, () -> vendorController.getById(1L, request));
        }

        @Test
        @DisplayName("Should return vendor by id")
        void shouldReturnVendorById() {
            clientRole.setViewPermissions(Collections.singleton(PermissionEntity.VENDORS_AND_CUSTOMERS));
            when(userService.whoami(request)).thenReturn(user);
            when(vendorService.findById(anyLong())).thenReturn(Optional.of(vendor));

            Vendor result = vendorController.getById(1L, request);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Get Mini Tests")
    class GetMiniTests {

        @Test
        @DisplayName("Should return mini vendors")
        void shouldReturnMiniVendors() {
            VendorMiniDTO miniDTO = new VendorMiniDTO();
            when(userService.whoami(request)).thenReturn(user);
            when(vendorService.findByCompany(anyLong())).thenReturn(Collections.singletonList(vendor));
            when(vendorMapper.toMiniDto(any(Vendor.class))).thenReturn(miniDTO);

            Collection<VendorMiniDTO> result = vendorController.getMini(request);

            assertFalse(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Create Tests")
    class CreateTests {

        @Test
        @DisplayName("Should throw forbidden if user has no permission")
        void shouldThrowForbiddenIfUserHasNoPermission() {
            clientRole.setCreatePermissions(new HashSet<>());
            when(userService.whoami(request)).thenReturn(user);

            assertThrows(CustomException.class, () -> vendorController.create(vendor, request));
        }

        @Test
        @DisplayName("Should create vendor successfully")
        void shouldCreateVendorSuccessfully() {
            clientRole.setCreatePermissions(Collections.singleton(PermissionEntity.VENDORS_AND_CUSTOMERS));
            when(userService.whoami(request)).thenReturn(user);
            when(vendorService.create(any(Vendor.class))).thenReturn(vendor);

            Vendor result = vendorController.create(vendor, request);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Patch Tests")
    class PatchTests {

        private VendorPatchDTO patchDTO;

        @BeforeEach
        void setup() {
            patchDTO = new VendorPatchDTO();
        }

        @Test
        @DisplayName("Should throw not found when vendor does not exist")
        void shouldThrowNotFoundWhenVendorDoesNotExist() {
            when(userService.whoami(request)).thenReturn(user);
            when(vendorService.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> vendorController.patch(patchDTO, 1L, request));
        }

        @Test
        @DisplayName("Should throw forbidden if user has no permission")
        void shouldThrowForbiddenIfUserHasNoPermission() {
            vendor.setCreatedBy(2L); // Not the creator
            clientRole.setEditOtherPermissions(new HashSet<>());
            when(userService.whoami(request)).thenReturn(user);
            when(vendorService.findById(anyLong())).thenReturn(Optional.of(vendor));

            assertThrows(CustomException.class, () -> vendorController.patch(patchDTO, 1L, request));
        }

        @Test
        @DisplayName("Should patch vendor with edit permission")
        void shouldPatchVendorWithEditPermission() {
            vendor.setCreatedBy(2L); // Not the creator
            clientRole.setEditOtherPermissions(Collections.singleton(PermissionEntity.VENDORS_AND_CUSTOMERS));
            when(userService.whoami(request)).thenReturn(user);
            when(vendorService.findById(anyLong())).thenReturn(Optional.of(vendor));
            when(vendorService.update(anyLong(), any(VendorPatchDTO.class))).thenReturn(vendor);

            Vendor result = vendorController.patch(patchDTO, 1L, request);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should patch vendor if user is creator")
        void shouldPatchVendorIfUserIsCreator() {
            when(userService.whoami(request)).thenReturn(user);
            when(vendorService.findById(anyLong())).thenReturn(Optional.of(vendor));
            when(vendorService.update(anyLong(), any(VendorPatchDTO.class))).thenReturn(vendor);

            Vendor result = vendorController.patch(patchDTO, 1L, request);

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
            when(vendorService.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> vendorController.delete(1L, request));
        }

        @Test
        @DisplayName("Should throw forbidden if user has no permission")
        void shouldThrowForbiddenIfUserHasNoPermission() {
            vendor.setCreatedBy(2L); // Not the creator
            clientRole.setDeleteOtherPermissions(new HashSet<>());
            when(userService.whoami(request)).thenReturn(user);
            when(vendorService.findById(anyLong())).thenReturn(Optional.of(vendor));

            assertThrows(CustomException.class, () -> vendorController.delete(1L, request));
        }

        @Test
        @DisplayName("Should delete vendor with delete permission")
        void shouldDeleteVendorWithDeletePermission() {
            vendor.setCreatedBy(2L); // Not the creator
            clientRole.setDeleteOtherPermissions(Collections.singleton(PermissionEntity.VENDORS_AND_CUSTOMERS));
            when(userService.whoami(request)).thenReturn(user);
            when(vendorService.findById(anyLong())).thenReturn(Optional.of(vendor));

            ResponseEntity response = vendorController.delete(1L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(vendorService).delete(1L);
        }

        @Test
        @DisplayName("Should delete vendor if user is creator")
        void shouldDeleteVendorIfUserIsCreator() {
            when(userService.whoami(request)).thenReturn(user);
            when(vendorService.findById(anyLong())).thenReturn(Optional.of(vendor));

            ResponseEntity response = vendorController.delete(1L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(vendorService).delete(1L);
        }
    }
}
