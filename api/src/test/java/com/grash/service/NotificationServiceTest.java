package com.grash.service;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.advancedsearch.SpecificationBuilder;
import com.grash.dto.NotificationPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.NotificationMapper;
import com.grash.model.Notification;
import com.grash.model.OwnUser;
import com.grash.model.PushNotificationToken;
import com.grash.model.enums.NotificationType;
import com.grash.repository.NotificationRepository;
import io.github.jav.exposerversdk.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private PushNotificationTokenService pushNotificationTokenService;
    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    @InjectMocks
    private NotificationService notificationService;

    private Notification notification;
    private OwnUser user;
    private NotificationPatchDTO notificationPatchDTO;

    @BeforeEach
    void setUp() {
        user = new OwnUser();
        user.setId(1L);
        user.setEmail("test@example.com");

        notification = new Notification("Test Message", user, NotificationType.TEAM, 10L);
        notification.setId(1L);

        notificationPatchDTO = new NotificationPatchDTO();
        notificationPatchDTO.setSeen(true);
    }

    @Nested
    @DisplayName("Notification Creation Tests")
    class CreateTests {
        @Test
        @DisplayName("Should create a single notification and send WebSocket message")
        void create_success() {
            when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
            doNothing().when(messagingTemplate).convertAndSend(anyString(), any(Notification.class));

            Notification createdNotification = notificationService.create(notification);

            assertNotNull(createdNotification);
            assertEquals(notification.getMessage(), createdNotification.getMessage());
            verify(notificationRepository, times(1)).save(notification);
            verify(messagingTemplate, times(1)).convertAndSend("/notifications/" + user.getId(), notification);
        }

        @Test
        @DisplayName("Should create multiple notifications and send WebSocket messages")
        void createMultiple_success() {
            Notification notification2 = new Notification("Another Message", user, NotificationType.ASSET, 20L);
            notification2.setId(2L);
            List<Notification> notifications = Arrays.asList(notification, notification2);

            when(notificationRepository.saveAll(anyIterable())).thenReturn(notifications);
            doNothing().when(messagingTemplate).convertAndSend(anyString(), any(Notification.class));

            notificationService.createMultiple(notifications, false, "Test Title");

            verify(notificationRepository, times(1)).saveAll(notifications);
            verify(messagingTemplate, times(1)).convertAndSend("/notifications/" + user.getId(), notification);
            verify(messagingTemplate, times(1)).convertAndSend("/notifications/" + user.getId(), notification2);
            // Push notifications are not sent when mobile is false
            verify(pushNotificationTokenService, never()).findByUser(anyLong());
        }

        @Test
        @DisplayName("Should create multiple notifications and send push notifications when mobile is true")
        void createMultiple_withPushNotifications() throws PushClientException, InterruptedException, ExecutionException {
            Notification notification2 = new Notification("Another Message", user, NotificationType.ASSET, 20L);
            notification2.setId(2L);
            List<Notification> notifications = Arrays.asList(notification, notification2);

            PushNotificationToken pushToken = new PushNotificationToken();
            pushToken.setToken("ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]");

            when(notificationRepository.saveAll(anyIterable())).thenReturn(notifications);
            doNothing().when(messagingTemplate).convertAndSend(anyString(), any(Notification.class));
            when(pushNotificationTokenService.findByUser(anyLong())).thenReturn(Optional.of(pushToken));

            notificationService.createMultiple(notifications, true, "Test Title");

            verify(notificationRepository, times(1)).saveAll(notifications);
            verify(messagingTemplate, times(1)).convertAndSend("/notifications/" + user.getId(), notification);
            verify(messagingTemplate, times(1)).convertAndSend("/notifications/" + user.getId(), notification2);
            verify(pushNotificationTokenService, times(2)).findByUser(user.getId());
        }

        @Test
        @DisplayName("Should not send push notifications if notifications list is empty")
        void createMultiple_emptyNotifications() {
            notificationService.createMultiple(Collections.emptyList(), true, "Test Title");

            verify(notificationRepository, times(1)).saveAll(argThat(iterable -> !iterable.iterator().hasNext()));
            verify(messagingTemplate, never()).convertAndSend(anyString(), any(Notification.class));
            verify(pushNotificationTokenService, never()).findByUser(anyLong());
        }

        @Test
        @DisplayName("Should not send push notifications if no valid tokens are found")
        void createMultiple_noValidTokens() throws PushClientException, InterruptedException, ExecutionException {
            List<Notification> notifications = Collections.singletonList(notification);

            notificationService.createMultiple(notifications, true, "Test Title");
        }
    }

    @Nested
    @DisplayName("Notification Update Tests")
    class UpdateTests {
        @Test
        @DisplayName("Should update an existing notification successfully")
        void update_success() {
            when(notificationRepository.existsById(anyLong())).thenReturn(true);
            when(notificationRepository.findById(anyLong())).thenReturn(Optional.of(notification));
            when(notificationMapper.updateNotification(any(Notification.class), any(NotificationPatchDTO.class))).thenReturn(notification);
            when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

            Notification updatedNotification = notificationService.update(1L, notificationPatchDTO);

            assertNotNull(updatedNotification);
            assertEquals(notification.isSeen(), updatedNotification.isSeen());
            verify(notificationRepository, times(1)).existsById(1L);
            verify(notificationRepository, times(1)).findById(1L);
            verify(notificationMapper, times(1)).updateNotification(notification, notificationPatchDTO);
            verify(notificationRepository, times(1)).save(notification);
        }

        @Test
        @DisplayName("Should throw CustomException when notification to update is not found")
        void update_notFound() {
            when(notificationRepository.existsById(anyLong())).thenReturn(false);

            CustomException exception = assertThrows(CustomException.class, () -> notificationService.update(1L, notificationPatchDTO));

            assertEquals("Not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
            verify(notificationRepository, times(1)).existsById(1L);
            verify(notificationRepository, never()).findById(anyLong());
            verify(notificationMapper, never()).updateNotification(any(Notification.class), any(NotificationPatchDTO.class));
            verify(notificationRepository, never()).save(any(Notification.class));
        }
    }

    @Nested
    @DisplayName("Notification Retrieval Tests")
    class RetrievalTests {
        @Test
        @DisplayName("Should return all notifications")
        void getAll_success() {
            List<Notification> notifications = Arrays.asList(notification, new Notification());
            when(notificationRepository.findAll()).thenReturn(notifications);

            Collection<Notification> result = notificationService.getAll();

            assertNotNull(result);
            assertEquals(2, result.size());
            verify(notificationRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Should find notification by ID when present")
        void findById_found() {
            when(notificationRepository.findById(anyLong())).thenReturn(Optional.of(notification));

            Optional<Notification> result = notificationService.findById(1L);

            assertTrue(result.isPresent());
            assertEquals(notification.getMessage(), result.get().getMessage());
            verify(notificationRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Should return empty optional when notification by ID is not found")
        void findById_notFound() {
            when(notificationRepository.findById(anyLong())).thenReturn(Optional.empty());

            Optional<Notification> result = notificationService.findById(1L);

            assertFalse(result.isPresent());
            verify(notificationRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Should find notifications by user ID")
        void findByUser_success() {
            List<Notification> notifications = Collections.singletonList(notification);
            when(notificationRepository.findByUser_Id(anyLong())).thenReturn(notifications);

            Collection<Notification> result = notificationService.findByUser(1L);

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(notificationRepository, times(1)).findByUser_Id(1L);
        }

        @Test
        @DisplayName("Should return a page of notifications based on search criteria")
        void findBySearchCriteria_success() {
            SearchCriteria searchCriteria = new SearchCriteria();
            searchCriteria.setPageNum(0);
            searchCriteria.setPageSize(10);
            searchCriteria.setSortField("id");
            searchCriteria.setDirection(org.springframework.data.domain.Sort.Direction.ASC);
            searchCriteria.setFilterFields(Collections.emptyList());

            Pageable pageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.Direction.ASC, "id");
            Page<Notification> notificationPage = new PageImpl<>(Collections.singletonList(notification), pageable, 1);

            when(((org.springframework.data.jpa.repository.JpaSpecificationExecutor<Notification>) notificationRepository).findAll(eq(null), any(Pageable.class))).thenReturn(notificationPage);

            Page<Notification> result = notificationService.findBySearchCriteria(searchCriteria);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals(notification.getMessage(), result.getContent().get(0).getMessage());
            verify((org.springframework.data.jpa.repository.JpaSpecificationExecutor<Notification>) notificationRepository, times(1)).findAll(isNull(), eq(pageable));
        }
    }

    @Nested
    @DisplayName("Notification Deletion Tests")
    class DeleteTests {
        @Test
        @DisplayName("Should delete a notification successfully")
        void delete_success() {
            doNothing().when(notificationRepository).deleteById(anyLong());

            notificationService.delete(1L);

            verify(notificationRepository, times(1)).deleteById(1L);
        }
    }

    @Nested
    @DisplayName("Read All Notifications Tests")
    class ReadAllTests {
        @Test
        @DisplayName("Should mark all notifications for a user as read")
        void readAll_success() {
            doNothing().when(notificationRepository).readAll(anyLong());

            notificationService.readAll(1L);

            verify(notificationRepository, times(1)).readAll(1L);
        }
    }

    @Nested
    @DisplayName("Send Push Notifications Tests")
    class SendPushNotificationsTests {
        private OwnUser user2;
        private PushNotificationToken pushToken1;
        private PushNotificationToken pushToken2;

        @BeforeEach
        void setUpPush() {
            user2 = new OwnUser();
            user2.setId(2L);
            user2.setEmail("test2@example.com");

            pushToken1 = new PushNotificationToken();
            pushToken1.setToken("ExponentPushToken[token1]");

            pushToken2 = new PushNotificationToken();
            pushToken2.setToken("ExponentPushToken[token2]");
        }

        @Test
        @DisplayName("Should send push notifications to valid tokens")
        void sendPushNotifications_validTokens() throws PushClientException, InterruptedException, ExecutionException {
            Collection<OwnUser> users = Arrays.asList(user, user2);
            Map<String, Object> data = new HashMap<>();
            data.put("key", "value");

            when(pushNotificationTokenService.findByUser(user.getId())).thenReturn(Optional.of(pushToken1));
            when(pushNotificationTokenService.findByUser(user2.getId())).thenReturn(Optional.of(pushToken2));

            notificationService.sendPushNotifications(users, "Test Title", "Test Message", data);

            verify(pushNotificationTokenService, times(1)).findByUser(user.getId());
            verify(pushNotificationTokenService, times(1)).findByUser(user2.getId());
        }

        @Test
        @DisplayName("Should not send push notifications if no valid tokens are found")
        void sendPushNotifications_noValidTokens() throws PushClientException, InterruptedException, ExecutionException {
            Collection<OwnUser> users = Collections.singletonList(user);
            Map<String, Object> data = new HashMap<>();
            data.put("key", "value");

            when(pushNotificationTokenService.findByUser(user.getId())).thenReturn(Optional.of(pushToken1));

            notificationService.sendPushNotifications(users, "Test Title", "Test Message", data);

            verify(pushNotificationTokenService, times(1)).findByUser(user.getId());
        }

        @Test
        @DisplayName("Should handle PushClientException during push notification sending")
        void sendPushNotifications_PushClientException() throws PushClientException, InterruptedException, ExecutionException {
            Collection<OwnUser> users = Collections.singletonList(user);
            Map<String, Object> data = new HashMap<>();
            data.put("key", "value");

            when(pushNotificationTokenService.findByUser(user.getId())).thenReturn(Optional.of(pushToken1));

            notificationService.sendPushNotifications(users, "Test Title", "Test Message", data);

            verify(pushNotificationTokenService, times(1)).findByUser(user.getId());
        }

        @Test
        @DisplayName("Should handle InterruptedException during push notification sending")
        void sendPushNotifications_InterruptedException() throws PushClientException, InterruptedException, ExecutionException {
            Collection<OwnUser> users = Collections.singletonList(user);
            Map<String, Object> data = new HashMap<>();
            data.put("key", "value");

            when(pushNotificationTokenService.findByUser(user.getId())).thenReturn(Optional.of(pushToken1));

            notificationService.sendPushNotifications(users, "Test Title", "Test Message", data);

            verify(pushNotificationTokenService, times(1)).findByUser(user.getId());
        }

        @Test
        @DisplayName("Should handle ExecutionException during push notification sending")
        void sendPushNotifications_ExecutionException() throws PushClientException, InterruptedException, ExecutionException {
            Collection<OwnUser> users = Collections.singletonList(user);
            Map<String, Object> data = new HashMap<>();
            data.put("key", "value");

            when(pushNotificationTokenService.findByUser(user.getId())).thenReturn(Optional.of(pushToken1));

            notificationService.sendPushNotifications(users, "Test Title", "Test Message", data);

            verify(pushNotificationTokenService, times(1)).findByUser(user.getId());
        }
    }
}
