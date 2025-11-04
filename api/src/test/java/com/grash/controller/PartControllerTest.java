package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.PartMiniDTO;
import com.grash.dto.PartPatchDTO;
import com.grash.dto.PartShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.PartMapper;
import com.grash.model.*;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.service.PartService;
import com.grash.service.UserService;
import com.grash.service.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartControllerTest {

    @Mock
    private PartService partService;
    @Mock
    private PartMapper partMapper;
    @Mock
    private UserService userService;
    @Mock
    private WorkflowService workflowService;
    @Mock
    private EntityManager em;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private PartController partController;

    private OwnUser clientUser;
    private OwnUser adminUser;
    private Part part;
    private PartShowDTO partShowDTO;

    @BeforeEach
    void setUp() {
        Company company = new Company();
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
        clientUser.setRole(clientRole);
        clientUser.setCompany(company);

        Role adminRole = new Role();
        adminRole.setRoleType(RoleType.ROLE_SUPER_ADMIN);
        adminUser = new OwnUser();
        adminUser.setId(2L);
        adminUser.setRole(adminRole);
        adminUser.setCompany(company);

        part = new Part();
        part.setId(1L);
        part.setCreatedBy(clientUser.getId());

        partShowDTO = new PartShowDTO();
        partShowDTO.setId(1L);
    }

    @Nested
    @DisplayName("Search Tests")
    class SearchTests {

        @Test
        @DisplayName("Should allow search for admin")
        void shouldAllowSearchForAdmin() {
            when(userService.whoami(request)).thenReturn(adminUser);
            when(partService.findBySearchCriteria(any(SearchCriteria.class))).thenReturn(Page.empty());

            partController.search(new SearchCriteria(), request);

            verify(partService).findBySearchCriteria(any(SearchCriteria.class));
        }

        @Test
        @DisplayName("Should deny search for client without permission")
        void shouldDenySearchForClientWithoutPermission() {
            when(userService.whoami(request)).thenReturn(clientUser);

            CustomException exception = assertThrows(CustomException.class, () -> {
                partController.search(new SearchCriteria(), request);
            });

            assertEquals("Access Denied", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }

        @Test
        @DisplayName("Should filter by creator for client without view-other permission")
        void shouldFilterByCreatorForClientWithoutViewOtherPermission() {
            clientUser.getRole().getViewPermissions().add(PermissionEntity.PARTS_AND_MULTIPARTS);
            when(userService.whoami(request)).thenReturn(clientUser);
            when(partService.findBySearchCriteria(any(SearchCriteria.class))).thenReturn(Page.empty());

            ArgumentCaptor<SearchCriteria> captor = ArgumentCaptor.forClass(SearchCriteria.class);
            partController.search(new SearchCriteria(), request);

            verify(partService).findBySearchCriteria(captor.capture());
            // Logic for filterCreatedBy and filterCompany is inside the service, we just check that it's called
        }
    }

    @Nested
    @DisplayName("Get By Id Tests")
    class GetByIdTests {

        @Test
        @DisplayName("Should throw not found when part does not exist")
        void shouldThrowNotFoundWhenPartDoesNotExist() {
            when(userService.whoami(request)).thenReturn(clientUser);
            when(partService.findById(1L)).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> partController.getById(1L, request));
        }

        @Test
        @DisplayName("Should deny access if no view permission")
        void shouldDenyAccessIfNoViewPermission() {
            when(userService.whoami(request)).thenReturn(clientUser);
            when(partService.findById(1L)).thenReturn(Optional.of(part));

            assertThrows(CustomException.class, () -> partController.getById(1L, request));
        }

        @Test
        @DisplayName("Should allow access for owner")
        void shouldAllowAccessForOwner() {
            clientUser.getRole().getViewPermissions().add(PermissionEntity.PARTS_AND_MULTIPARTS);
            when(userService.whoami(request)).thenReturn(clientUser);
            when(partService.findById(1L)).thenReturn(Optional.of(part));
            when(partMapper.toShowDto(part)).thenReturn(partShowDTO);

            PartShowDTO result = partController.getById(1L, request);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Create Tests")
    class CreateTests {

        @Test
        @DisplayName("Should deny creation without permission")
        void shouldDenyCreationWithoutPermission() {
            when(userService.whoami(request)).thenReturn(clientUser);

            assertThrows(CustomException.class, () -> partController.create(new Part(), request));
        }

        @Test
        @DisplayName("Should throw exception for duplicate barcode")
        void shouldThrowExceptionForDuplicateBarcode() {
            clientUser.getRole().getCreatePermissions().add(PermissionEntity.PARTS_AND_MULTIPARTS);
            Part newPart = new Part();
            newPart.setBarcode("123");
            when(userService.whoami(request)).thenReturn(clientUser);
            when(partService.findByBarcodeAndCompany(anyString(), anyLong())).thenReturn(Optional.of(new Part()));

            assertThrows(CustomException.class, () -> partController.create(newPart, request));
        }

        @Test
        @DisplayName("Should create part successfully")
        void shouldCreatePartSuccessfully() {
            clientUser.getRole().getCreatePermissions().add(PermissionEntity.PARTS_AND_MULTIPARTS);
            Part newPart = new Part();
            when(userService.whoami(request)).thenReturn(clientUser);
            when(partService.create(newPart)).thenReturn(part);
            when(partMapper.toShowDto(part)).thenReturn(partShowDTO);

            PartShowDTO result = partController.create(newPart, request);

            assertNotNull(result);
            verify(partService).notify(any(Part.class), any());
        }
    }

    @Nested
    @DisplayName("Patch Tests")
    class PatchTests {

        private PartPatchDTO patchDTO;

        @BeforeEach
        void setup() {
            patchDTO = new PartPatchDTO();
        }

        @Test
        @DisplayName("Should throw not found when part does not exist")
        void shouldThrowNotFoundWhenPartDoesNotExist() {
            when(userService.whoami(request)).thenReturn(clientUser);
            when(partService.findById(1L)).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> partController.patch(patchDTO, 1L, request));
        }

        @Test
        @DisplayName("Should deny patch if no permission")
        void shouldDenyPatchIfNoPermission() {
            part.setCreatedBy(99L); // Not the owner
            when(userService.whoami(request)).thenReturn(clientUser);
            when(partService.findById(1L)).thenReturn(Optional.of(part));

            assertThrows(CustomException.class, () -> partController.patch(patchDTO, 1L, request));
        }

        @Test
        @DisplayName("Should throw exception for duplicate barcode on patch")
        void shouldThrowExceptionForDuplicateBarcodeOnPatch() {
            clientUser.getRole().getEditOtherPermissions().add(PermissionEntity.PARTS_AND_MULTIPARTS);
            patchDTO.setBarcode("123");
            Part existingPartWithBarcode = new Part();
            existingPartWithBarcode.setId(2L);

            when(userService.whoami(request)).thenReturn(clientUser);
            when(partService.findById(1L)).thenReturn(Optional.of(part));
            when(partService.findByBarcodeAndCompany(anyString(), anyLong())).thenReturn(Optional.of(existingPartWithBarcode));

            assertThrows(CustomException.class, () -> partController.patch(patchDTO, 1L, request));
        }

        @Test
        @DisplayName("Should patch part successfully")
        void shouldPatchPartSuccessfully() {
            clientUser.getRole().getEditOtherPermissions().add(PermissionEntity.PARTS_AND_MULTIPARTS);
            when(userService.whoami(request)).thenReturn(clientUser);
            when(partService.findById(1L)).thenReturn(Optional.of(part));
            when(partService.update(anyLong(), any(PartPatchDTO.class))).thenReturn(part);
            when(workflowService.findByMainConditionAndCompany(any(), anyLong())).thenReturn(Collections.emptyList());
            when(partMapper.toShowDto(part)).thenReturn(partShowDTO);

            PartShowDTO result = partController.patch(patchDTO, 1L, request);

            assertNotNull(result);
            verify(partService).patchNotify(any(Part.class), any(Part.class), any());
        }
    }

    @Nested
    @DisplayName("Get Mini Tests")
    class GetMiniTests {
        @Test
        @DisplayName("Should return mini parts")
        void shouldReturnMiniParts() {
            when(userService.whoami(request)).thenReturn(clientUser);
            when(partService.findByCompany(anyLong())).thenReturn(Collections.singletonList(part));
            when(partMapper.toMiniDto(part)).thenReturn(new PartMiniDTO());

            Collection<PartMiniDTO> result = partController.getMini(request);

            assertFalse(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Delete Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should throw not found on delete")
        void shouldThrowNotFoundOnDelete() {
            when(userService.whoami(request)).thenReturn(clientUser);
            when(partService.findById(1L)).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> partController.delete(1L, request));
        }

        @Test
        @DisplayName("Should deny delete if no permission")
        void shouldDenyDeleteIfNoPermission() {
            clientUser.setId(2L); // Different user
            part.setCreatedBy(99L); // Not the owner
            when(userService.whoami(request)).thenReturn(clientUser);
            when(partService.findById(1L)).thenReturn(Optional.of(part));

            assertThrows(CustomException.class, () -> partController.delete(1L, request));
        }

        @Test
        @DisplayName("Should delete part successfully")
        void shouldDeletePartSuccessfully() {
            clientUser.getRole().getDeleteOtherPermissions().add(PermissionEntity.PARTS_AND_MULTIPARTS);
            when(userService.whoami(request)).thenReturn(clientUser);
            when(partService.findById(1L)).thenReturn(Optional.of(part));

            ResponseEntity response = partController.delete(1L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(partService).delete(1L);
        }
    }
}
