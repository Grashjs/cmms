package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.FilePatchDTO;
import com.grash.dto.FileShowDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.factory.StorageServiceFactory;
import com.grash.mapper.FileMapper;
import com.grash.model.*;
import com.grash.model.enums.FileType;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PlanFeatures;
import com.grash.model.enums.RoleType;
import com.grash.service.FileService;
import com.grash.service.StorageService;
import com.grash.service.TaskService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock
    private StorageServiceFactory storageServiceFactory;

    @Mock
    private FileService fileService;

    @Mock
    private UserService userService;

    @Mock
    private TaskService taskService;

    @Mock
    private FileMapper fileMapper;

    @Mock
    private HttpServletRequest request;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private FileController fileController;

    private OwnUser clientUser;
    private File file;
    private FileShowDTO fileShowDTO;

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
        subscriptionPlan.setFeatures(new HashSet<>(Arrays.asList(PlanFeatures.FILE)));
        subscription.setSubscriptionPlan(subscriptionPlan);
        company.setSubscription(subscription);
        clientUser.setCompany(company);

        file = new File();
        file.setId(1L);
        file.setCreatedBy(clientUser.getId());

        fileShowDTO = new FileShowDTO();
        fileShowDTO.setId(1L);
    }

    @Nested
    @DisplayName("handleFileUpload method")
    class HandleFileUploadTests {

        @Test
        @DisplayName("Should upload file for user with create permission and feature")
        void handleFileUpload_withPermissionAndFeature_shouldSucceed() {
            clientUser.getRole().setCreatePermissions(new HashSet<>(Arrays.asList(PermissionEntity.FILES)));
            when(userService.whoami(request)).thenReturn(clientUser);
            when(storageServiceFactory.getStorageService()).thenReturn(storageService);
            when(storageService.upload(any(MultipartFile.class), any(String.class))).thenReturn("filePath");
            when(fileService.create(any(File.class))).thenReturn(file);
            when(fileMapper.toShowDto(any(File.class))).thenReturn(fileShowDTO);

            MultipartFile multipartFile = new MockMultipartFile("file", "test.txt", "text/plain", "test".getBytes());
            List<FileShowDTO> result = fileController.handleFileUpload(new MultipartFile[]{multipartFile}, "folder", "false", request, FileType.OTHER, null);

            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should throw exception for user without create permission")
        void handleFileUpload_withoutPermission_shouldThrowException() {
            clientUser.getRole().setCreatePermissions(Collections.emptySet());
            when(userService.whoami(request)).thenReturn(clientUser);

            MultipartFile multipartFile = new MockMultipartFile("file", "test.txt", "text/plain", "test".getBytes());
            CustomException exception = assertThrows(CustomException.class, () -> fileController.handleFileUpload(new MultipartFile[]{multipartFile}, "folder", "false", request, FileType.OTHER, null));

            assertEquals("Access Denied", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("search method")
    class SearchTests {

        @Test
        @DisplayName("Should return files for client with view permission")
        void search_asClientWithPermission_shouldSucceed() {
            clientUser.getRole().setViewPermissions(new HashSet<>(Arrays.asList(PermissionEntity.FILES)));
            when(userService.whoami(request)).thenReturn(clientUser);
            Page<File> page = new PageImpl<>(Arrays.asList(file));
            when(fileService.findBySearchCriteria(any(SearchCriteria.class))).thenReturn(page);
            when(fileMapper.toShowDto(any(File.class))).thenReturn(fileShowDTO);

            ResponseEntity<Page<FileShowDTO>> response = fileController.search(new SearchCriteria(), request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().getTotalElements());
        }

        @Test
        @DisplayName("Should throw exception for client without view permission")
        void search_asClientWithoutPermission_shouldThrowException() {
            clientUser.getRole().setViewPermissions(Collections.emptySet());
            when(userService.whoami(request)).thenReturn(clientUser);

            CustomException exception = assertThrows(CustomException.class, () -> fileController.search(new SearchCriteria(), request));

            assertEquals("Access Denied", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("getById method")
    class GetByIdTests {

        @Test
        @DisplayName("Should return file for user with view permission")
        void getById_withPermission_shouldSucceed() {
            clientUser.getRole().setViewPermissions(new HashSet<>(Arrays.asList(PermissionEntity.FILES)));
            when(userService.whoami(request)).thenReturn(clientUser);
            when(fileService.findById(1L)).thenReturn(Optional.of(file));
            when(fileMapper.toShowDto(file)).thenReturn(fileShowDTO);

            FileShowDTO result = fileController.getById(1L, request);

            assertNotNull(result);
            assertEquals(fileShowDTO.getId(), result.getId());
        }

        @Test
        @DisplayName("Should throw exception when file not found")
        void getById_notFound_shouldThrowException() {
            when(userService.whoami(request)).thenReturn(clientUser);
            when(fileService.findById(1L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class, () -> fileController.getById(1L, request));

            assertEquals("Not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }

        @Test
        @DisplayName("Should throw exception for user without view permission")
        void getById_withoutPermission_shouldThrowException() {
            clientUser.getRole().setViewPermissions(Collections.emptySet());
            when(userService.whoami(request)).thenReturn(clientUser);
            when(fileService.findById(1L)).thenReturn(Optional.of(file));

            CustomException exception = assertThrows(CustomException.class, () -> fileController.getById(1L, request));

            assertEquals("Access denied", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("patch method")
    class PatchTests {

        private FilePatchDTO patchDTO;

        @BeforeEach
        void patchSetup() {
            patchDTO = new FilePatchDTO();
            patchDTO.setName("Updated Name");
        }

        @Test
        @DisplayName("Should patch file for user with edit permission")
        void patch_withPermission_shouldSucceed() {
            clientUser.getRole().setEditOtherPermissions(new HashSet<>(Arrays.asList(PermissionEntity.FILES)));
            when(userService.whoami(request)).thenReturn(clientUser);
            when(fileService.findById(1L)).thenReturn(Optional.of(file));
            when(fileService.update(any(File.class))).thenReturn(file);
            when(fileMapper.toShowDto(file)).thenReturn(fileShowDTO);

            FileShowDTO result = fileController.patch(patchDTO, 1L, request);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should throw exception when file not found")
        void patch_notFound_shouldThrowException() {
            when(userService.whoami(request)).thenReturn(clientUser);
            when(fileService.findById(1L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class, () -> fileController.patch(patchDTO, 1L, request));

            assertEquals("File not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }

        @Test
        @DisplayName("Should throw exception for user without edit permission")
        void patch_withoutPermission_shouldThrowException() {
            file.setCreatedBy(2L); // Different creator
            clientUser.getRole().setEditOtherPermissions(Collections.emptySet());
            when(userService.whoami(request)).thenReturn(clientUser);
            when(fileService.findById(1L)).thenReturn(Optional.of(file));

            CustomException exception = assertThrows(CustomException.class, () -> fileController.patch(patchDTO, 1L, request));

            assertEquals("Forbidden", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("delete method")
    class DeleteTests {

        @Test
        @DisplayName("Should delete file if user is creator")
        void delete_asCreator_shouldSucceed() {
            when(userService.whoami(request)).thenReturn(clientUser);
            when(fileService.findById(1L)).thenReturn(Optional.of(file));

            ResponseEntity<SuccessResponse> response = fileController.delete(1L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().isSuccess());
            assertEquals("Deleted successfully", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Should throw exception when file not found")
        void delete_notFound_shouldThrowException() {
            when(userService.whoami(request)).thenReturn(clientUser);
            when(fileService.findById(1L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class, () -> fileController.delete(1L, request));

            assertEquals("File not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }

        @Test
        @DisplayName("Should throw exception if user is not creator and no delete other permission")
        void delete_withoutPermission_shouldThrowException() {
            file.setCreatedBy(2L); // Different creator
            clientUser.getRole().setDeleteOtherPermissions(Collections.emptySet());
            when(userService.whoami(request)).thenReturn(clientUser);
            when(fileService.findById(1L)).thenReturn(Optional.of(file));

            CustomException exception = assertThrows(CustomException.class, () -> fileController.delete(1L, request));

            assertEquals("Forbidden", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }
    }
}