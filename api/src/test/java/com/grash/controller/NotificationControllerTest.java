package com.grash.controller;

import com.grash.advancedsearch.FilterField;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.NotificationPatchDTO;
import com.grash.dto.PushTokenPayload;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.Notification;
import com.grash.model.OwnUser;
import com.grash.model.PushNotificationToken;
import com.grash.model.Role;
import com.grash.model.enums.RoleType;
import com.grash.service.NotificationService;
import com.grash.service.PushNotificationTokenService;
import com.grash.service.UserService;
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

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserService userService;

    @Mock
    private PushNotificationTokenService pushNotificationTokenService;

    @InjectMocks
    private NotificationController notificationController;

    @Mock
    private HttpServletRequest request;

    private OwnUser clientUser;
    private OwnUser adminUser;

    @BeforeEach
    void setUp() {
        clientUser = new OwnUser();
        clientUser.setId(1L);
        Role clientRole = new Role();
        clientRole.setRoleType(RoleType.ROLE_CLIENT);
        clientUser.setRole(clientRole);

        adminUser = new OwnUser();
        adminUser.setId(2L);
        Role adminRole = new Role();
        adminRole.setRoleType(RoleType.ROLE_SUPER_ADMIN);
        adminUser.setRole(adminRole);
    }

    @Nested
    @DisplayName("getAll tests")
    class GetAllTests {

        @Test
        @DisplayName("should return user notifications for client")
        void shouldReturnUserNotificationsForClient() {
            when(userService.whoami(request)).thenReturn(clientUser);
            when(notificationService.findByUser(clientUser.getId())).thenReturn(Collections.singletonList(new Notification()));
            Collection<Notification> result = notificationController.getAll(request);

            assertFalse(result.isEmpty());
            verify(notificationService).findByUser(clientUser.getId());
            verify(notificationService, never()).getAll();
        }

        @Test
        @DisplayName("should return all notifications for non-client")
        void shouldReturnAllNotificationsForNonClient() {
            when(userService.whoami(request)).thenReturn(adminUser);
            when(notificationService.getAll()).thenReturn(Collections.singletonList(new Notification()));

            Collection<Notification> result = notificationController.getAll(request);

            assertFalse(result.isEmpty());
            verify(notificationService).getAll();
            verify(notificationService, never()).findByUser(anyLong());
        }
    }

    @Nested
    @DisplayName("search tests")
    class SearchTests {

        @Test
        @DisplayName("should add user filter for client")
        void shouldAddUserFilterForClient() {
            when(userService.whoami(request)).thenReturn(clientUser);
            SearchCriteria searchCriteria = new SearchCriteria();
            searchCriteria.setFilterFields(new ArrayList<>());
            Page<Notification> page = new PageImpl<>(Collections.singletonList(new Notification()));
            when(notificationService.findBySearchCriteria(any(SearchCriteria.class))).thenReturn(page);

            notificationController.search(searchCriteria, request);

            ArgumentCaptor<SearchCriteria> captor = ArgumentCaptor.forClass(SearchCriteria.class);
            verify(notificationService).findBySearchCriteria(captor.capture());
            SearchCriteria capturedCriteria = captor.getValue();
            assertEquals(1, capturedCriteria.getFilterFields().size());
            FilterField filterField = capturedCriteria.getFilterFields().get(0);
            assertEquals("user", filterField.getField());
            assertEquals(clientUser.getId(), filterField.getValue());
        }

        @Test
        @DisplayName("should not add user filter for non-client")
        void shouldNotAddUserFilterForNonClient() {
            when(userService.whoami(request)).thenReturn(adminUser);
            SearchCriteria searchCriteria = new SearchCriteria();
            searchCriteria.setFilterFields(new ArrayList<>());
            Page<Notification> page = new PageImpl<>(Collections.singletonList(new Notification()));
            when(notificationService.findBySearchCriteria(any(SearchCriteria.class))).thenReturn(page);

            notificationController.search(searchCriteria, request);

            ArgumentCaptor<SearchCriteria> captor = ArgumentCaptor.forClass(SearchCriteria.class);
            verify(notificationService).findBySearchCriteria(captor.capture());
            SearchCriteria capturedCriteria = captor.getValue();
            assertTrue(capturedCriteria.getFilterFields().isEmpty());
        }
    }

    @Nested
    @DisplayName("readAll tests")
    class ReadAllTests {

        @Test
        @DisplayName("should call readAll service method")
        void shouldCallReadAllServiceMethod() {
            when(userService.whoami(request)).thenReturn(clientUser);

            SuccessResponse response = notificationController.readAll(request);

            assertTrue(response.isSuccess());
            assertEquals("Notifications read", response.getMessage());
            verify(notificationService).readAll(clientUser.getId());
        }
    }

    @Nested
    @DisplayName("getById tests")
    class GetByIdTests {

        @Test
        @DisplayName("should return notification when found")
        void shouldReturnNotificationWhenFound() {
            Notification notification = new Notification();
            notification.setId(1L);
            when(userService.whoami(request)).thenReturn(clientUser);
            when(notificationService.findById(1L)).thenReturn(Optional.of(notification));

            Notification result = notificationController.getById(1L, request);

            assertNotNull(result);
            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("should throw CustomException when not found")
        void shouldThrowCustomExceptionWhenNotFound() {
            when(userService.whoami(request)).thenReturn(clientUser);
            when(notificationService.findById(1L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class, () -> {
                notificationController.getById(1L, request);
            });

            assertEquals("Not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("patch tests")
    class PatchTests {

        private NotificationPatchDTO patchDTO;

        @BeforeEach
        void setUp() {
            patchDTO = new NotificationPatchDTO();
        }

        @Test
        @DisplayName("should patch notification when found")
        void shouldPatchNotificationWhenFound() {
            Notification notification = new Notification();
            notification.setId(1L);
            when(userService.whoami(request)).thenReturn(clientUser);
            when(notificationService.findById(1L)).thenReturn(Optional.of(notification));
            when(notificationService.update(1L, patchDTO)).thenReturn(notification);

            Notification result = notificationController.patch(patchDTO, 1L, request);

            assertNotNull(result);
            verify(notificationService).update(1L, patchDTO);
        }

        @Test
        @DisplayName("should throw CustomException when not found")
        void shouldThrowCustomExceptionWhenNotFound() {
            when(userService.whoami(request)).thenReturn(clientUser);
            when(notificationService.findById(1L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class, () -> {
                notificationController.patch(patchDTO, 1L, request);
            });

            assertEquals("Notification not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("savePushToken tests")
    class SavePushTokenTests {

        private PushTokenPayload tokenPayload;

        @BeforeEach
        void setUp() {
            tokenPayload = new PushTokenPayload();
            tokenPayload.setToken("test-token");
        }

        @Test
        @DisplayName("should update existing token")
        void shouldUpdateExistingToken() {
            PushNotificationToken existingToken = new PushNotificationToken();
            when(userService.whoami(request)).thenReturn(clientUser);
            when(pushNotificationTokenService.findByUser(clientUser.getId())).thenReturn(Optional.of(existingToken));

            SuccessResponse response = notificationController.savePushToken(tokenPayload, request);

            assertTrue(response.isSuccess());
            assertEquals("Ok", response.getMessage());
            verify(pushNotificationTokenService).save(existingToken);
            assertEquals("test-token", existingToken.getToken());
        }

        @Test
        @DisplayName("should create new token")
        void shouldCreateNewToken() {
            when(userService.whoami(request)).thenReturn(clientUser);
            when(pushNotificationTokenService.findByUser(clientUser.getId())).thenReturn(Optional.empty());

            SuccessResponse response = notificationController.savePushToken(tokenPayload, request);

            assertTrue(response.isSuccess());
            assertEquals("Ok", response.getMessage());
            ArgumentCaptor<PushNotificationToken> captor = ArgumentCaptor.forClass(PushNotificationToken.class);
            verify(pushNotificationTokenService).save(captor.capture());
            PushNotificationToken newToken = captor.getValue();
            assertEquals("test-token", newToken.getToken());
            assertEquals(clientUser, newToken.getUser());
        }
    }
}
