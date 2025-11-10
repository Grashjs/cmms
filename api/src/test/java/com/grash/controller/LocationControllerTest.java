package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.LocationPatchDTO;
import com.grash.dto.LocationShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.LocationMapper;
import com.grash.model.Company;
import com.grash.model.Location;
import com.grash.model.OwnUser;
import com.grash.model.Role;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.service.LocationService;
import com.grash.service.UserService;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationControllerTest {

    @InjectMocks
    private LocationController locationController;

    @Mock
    private LocationService locationService;

    @Mock
    private LocationMapper locationMapper;

    @Mock
    private UserService userService;

    @Mock
    private EntityManager em;

    @Mock
    private HttpServletRequest request;

    private OwnUser clientUser;
    private OwnUser superAdminUser;
    private Company company;
    private Location location;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);

        Role clientRole = new Role();
        clientRole.setRoleType(RoleType.ROLE_CLIENT);
        clientRole.setViewPermissions(new HashSet<>());
        clientRole.setViewOtherPermissions(new HashSet<>());
        clientRole.setCreatePermissions(new HashSet<>());
        clientRole.setEditOtherPermissions(new HashSet<>());
        clientRole.setDeleteOtherPermissions(new HashSet<>());

        clientUser = new OwnUser();
        clientUser.setId(1L);
        clientUser.setCompany(company);
        clientUser.setRole(clientRole);

        Role superAdminRole = new Role();
        superAdminRole.setRoleType(RoleType.ROLE_SUPER_ADMIN);
        superAdminUser = new OwnUser();
        superAdminUser.setId(2L);
        superAdminUser.setRole(superAdminRole);

        location = new Location();
        location.setId(10L);
        location.setCreatedBy(clientUser.getId());
        location.setCompany(company);
    }

    private void addPermission(PermissionEntity permission, Set<PermissionEntity>... permissionSets) {
        for (Set<PermissionEntity> permissionSet : permissionSets) {
            permissionSet.add(permission);
        }
    }

    @Nested
    @DisplayName("getAll Tests")
    class GetAllTests {
        @Test
        void getAll_asSuperAdmin_shouldReturnAll() {
            when(userService.whoami(request)).thenReturn(superAdminUser);
            when(locationService.getAll()).thenReturn(Collections.singletonList(location));

            locationController.getAll(request);

            verify(locationService, times(1)).getAll();
            verify(locationMapper, times(1)).toShowDto(any(Location.class), any(LocationService.class));
        }

        @Test
        void getAll_asClientWithPermission_shouldReturnCompanyLocations() {
            addPermission(PermissionEntity.LOCATIONS, clientUser.getRole().getViewPermissions());
            when(userService.whoami(request)).thenReturn(clientUser);
            when(locationService.findByCompany(company.getId())).thenReturn(Collections.singletonList(location));

            locationController.getAll(request);

            verify(locationService, times(1)).findByCompany(company.getId());
            verify(locationMapper, times(1)).toShowDto(any(Location.class), any(LocationService.class));
        }

        @Test
        void getAll_asClientWithoutPermission_shouldThrowForbidden() {
            when(userService.whoami(request)).thenReturn(clientUser);

            CustomException exception = assertThrows(CustomException.class, () -> locationController.getAll(request));
            assertEquals("Access Denied", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("search Tests")
    class SearchTests {
        @Test
        void search_asClientWithViewPermission_shouldFilterByCompany() {
            addPermission(PermissionEntity.LOCATIONS, clientUser.getRole().getViewPermissions());
            when(userService.whoami(request)).thenReturn(clientUser);
            SearchCriteria criteria = new SearchCriteria();
            Page<LocationShowDTO> page = new PageImpl<>(Collections.emptyList());
            when(locationService.findBySearchCriteria(criteria)).thenReturn(page);

            ResponseEntity<Page<LocationShowDTO>> response = locationController.search(criteria, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
        }

        @Test
        void search_asClientWithoutViewPermission_shouldThrowForbidden() {
            when(userService.whoami(request)).thenReturn(clientUser);
            SearchCriteria criteria = new SearchCriteria();

            CustomException exception = assertThrows(CustomException.class, () -> locationController.search(criteria, request));
            assertEquals("Access Denied", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("getChildrenById Tests")
    class GetChildrenByIdTests {
        @Test
        void getChildrenById_asClientForRoot_shouldReturnRootLocations() {
            addPermission(PermissionEntity.LOCATIONS, clientUser.getRole().getViewPermissions());
            when(userService.whoami(request)).thenReturn(clientUser);
            Pageable pageable = PageRequest.of(0, 10, Sort.by("name"));
            when(locationService.findByCompany(company.getId(), pageable.getSort())).thenReturn(Collections.singletonList(location));

            locationController.getChildrenById(0L, pageable, request);

            verify(locationService, times(1)).findByCompany(company.getId(), pageable.getSort());
        }

        @Test
        void getChildrenById_locationNotFound_shouldThrowNotFound() {
            when(userService.whoami(request)).thenReturn(clientUser);
            when(locationService.findById(anyLong())).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class, () -> locationController.getChildrenById(1L, PageRequest.of(0, 10), request));
            assertEquals("Not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("getMini Tests")
    class GetMiniTests {
        @Test
        void getMini_asClient_shouldReturnMiniDTOs() {
            when(userService.whoami(request)).thenReturn(clientUser);
            when(locationService.findByCompany(company.getId())).thenReturn(Collections.singletonList(location));

            locationController.getMini(request);

            verify(locationService, times(1)).findByCompany(company.getId());
            verify(locationMapper, times(1)).toMiniDto(any(Location.class));
        }
    }

    @Nested
    @DisplayName("getById Tests")
    class GetByIdTests {
        @Test
        void getById_withPermission_shouldReturnLocation() {
            addPermission(PermissionEntity.LOCATIONS, clientUser.getRole().getViewPermissions());
            when(userService.whoami(request)).thenReturn(clientUser);
            when(locationService.findById(location.getId())).thenReturn(Optional.of(location));

            locationController.getById(location.getId(), request);

            verify(locationMapper, times(1)).toShowDto(any(Location.class), any(LocationService.class));
        }

        @Test
        void getById_locationNotFound_shouldThrowNotFound() {
            when(userService.whoami(request)).thenReturn(clientUser);
            when(locationService.findById(anyLong())).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class, () -> locationController.getById(1L, request));
            assertEquals("Not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("create Tests")
    class CreateTests {
        @Test
        void create_withPermission_shouldCreateLocation() {
            addPermission(PermissionEntity.LOCATIONS, clientUser.getRole().getCreatePermissions());
            when(userService.whoami(request)).thenReturn(clientUser);
            when(locationService.create(any(Location.class), any(Company.class))).thenReturn(location);

            locationController.create(location, request);

            verify(locationService, times(1)).create(any(Location.class), any(Company.class));
            verify(locationMapper, times(1)).toShowDto(any(Location.class), any(LocationService.class));
        }

        @Test
        void create_withoutPermission_shouldThrowForbidden() {
            when(userService.whoami(request)).thenReturn(clientUser);

            CustomException exception = assertThrows(CustomException.class, () -> locationController.create(location, request));
            assertEquals("Access denied", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("patch Tests")
    class PatchTests {
        @Test
        void patch_withPermission_shouldUpdateLocation() {
            when(userService.whoami(request)).thenReturn(clientUser);
            when(locationService.findById(location.getId())).thenReturn(Optional.of(location));
            when(locationService.update(anyLong(), any(LocationPatchDTO.class))).thenReturn(location);
            LocationPatchDTO patchDTO = new LocationPatchDTO();

            locationController.patch(patchDTO, location.getId(), request);

            verify(locationService, times(1)).update(anyLong(), any(LocationPatchDTO.class));
        }

        @Test
        void patch_parentAsSelf_shouldThrowNotAcceptable() {
            when(userService.whoami(request)).thenReturn(clientUser);
            when(locationService.findById(location.getId())).thenReturn(Optional.of(location));
            LocationPatchDTO patchDTO = new LocationPatchDTO();
            Location parent = new Location();
            parent.setId(location.getId());
            patchDTO.setParentLocation(parent);

            CustomException exception = assertThrows(CustomException.class, () -> locationController.patch(patchDTO, location.getId(), request));
            assertEquals("Parent location cannot be the same id", exception.getMessage());
            assertEquals(HttpStatus.NOT_ACCEPTABLE, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("delete Tests")
    class DeleteTests {
        @Test
        void delete_withPermission_shouldDelete() {
            when(userService.whoami(request)).thenReturn(clientUser);
            when(locationService.findById(location.getId())).thenReturn(Optional.of(location));

            ResponseEntity response = locationController.delete(location.getId(), request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(locationService, times(1)).delete(location.getId());
        }

        @Test
        void delete_withoutPermission_shouldThrowForbidden() {
            OwnUser anotherUser = new OwnUser();
            anotherUser.setId(99L);
            location.setCreatedBy(anotherUser.getId());
            when(userService.whoami(request)).thenReturn(clientUser);
            when(locationService.findById(location.getId())).thenReturn(Optional.of(location));

            CustomException exception = assertThrows(CustomException.class, () -> locationController.delete(location.getId(), request));
            assertEquals("Forbidden", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }
    }
}
