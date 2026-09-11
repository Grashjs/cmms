package com.grash.service;

import com.grash.model.Notification;
import com.grash.model.Role;
import com.grash.model.User;
import com.grash.model.enums.NotificationType;
import com.grash.model.enums.RoleType;
import com.grash.repository.NotificationRepository;
import com.grash.utils.Helper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private PushNotificationTokenService pushNotificationTokenService;
    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    private User currentUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        currentUser = buildUser(1L, "current@test.com");
        otherUser  = buildUser(2L, "other@test.com");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private User buildUser(Long id, String email) {
        User u = new User();
        u.setId(id);
        u.setFirstName("User" + id);
        u.setLastName("Last" + id);
        u.setEmail(email);
        Role role = new Role();
        role.setRoleType(RoleType.ROLE_CLIENT);
        u.setRole(role);
        return u;
    }

    private Notification notification(User user, String message, NotificationType type, Long resourceId) {
        return new Notification(message, user, type, resourceId);
    }

    // ── createMultiple ───────────────────────────────────────────────────

    @Nested
    class CreateMultiple {

        @Test
        void emptyList_returnsEarly() {
            notificationService.createMultiple(Collections.emptyList(), true, "Title");

            verify(notificationRepository, never()).saveAll(anyList());
            verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
        }

        @Test
        void noAuthenticatedUser_sendsAllNotifications() {
            SecurityContextHolder.clearContext();
            Notification n1 = notification(currentUser, "m1", NotificationType.WORK_ORDER, 10L);
            Notification n2 = notification(otherUser, "m2", NotificationType.WORK_ORDER, 10L);
            List<Notification> input = new ArrayList<>(List.of(n1, n2));
            when(notificationRepository.saveAll(eq(input))).thenReturn(input);

            notificationService.createMultiple(input, false, "Title");

            ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
            verify(notificationRepository).saveAll(captor.capture());
            assertEquals(2, captor.getValue().size());

            verify(messagingTemplate).convertAndSendToUser(eq("current@test.com"), eq("/notifications"), eq(n1));
            verify(messagingTemplate).convertAndSendToUser(eq("other@test.com"), eq("/notifications"), eq(n2));
        }

        @Test
        void excludesCurrentUserFromSaveAndWebSocket() {
            Helper.setCurrentUser(currentUser);
            Notification n1 = notification(currentUser, "m1", NotificationType.WORK_ORDER, 10L);
            Notification n2 = notification(otherUser, "m2", NotificationType.WORK_ORDER, 10L);
            List<Notification> input = new ArrayList<>(List.of(n1, n2));
            when(notificationRepository.saveAll(eq(List.of(n2)))).thenReturn(List.of(n2));

            notificationService.createMultiple(input, false, "Title");

            ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
            verify(notificationRepository).saveAll(captor.capture());
            assertEquals(1, captor.getValue().size());
            assertEquals(2L, ((Notification) captor.getValue().get(0)).getUser().getId());

            verify(messagingTemplate).convertAndSendToUser(eq("other@test.com"), eq("/notifications"), eq(n2));
            verify(messagingTemplate, never()).convertAndSendToUser(eq("current@test.com"), anyString(), any());
        }

        @Test
        void allRecipientsAreCurrentUser_skipsCompletely() {
            Helper.setCurrentUser(currentUser);
            Notification n1 = notification(currentUser, "m1", NotificationType.WORK_ORDER, 10L);
            List<Notification> input = List.of(n1);

            notificationService.createMultiple(input, false, "Title");

            verify(notificationRepository, never()).saveAll(anyList());
            verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
        }

        @Test
        void excludesCurrentUserFromPushRecipients() {
            Helper.setCurrentUser(currentUser);
            Notification n1 = notification(currentUser, "m1", NotificationType.WORK_ORDER, 10L);
            Notification n2 = notification(otherUser, "m2", NotificationType.WORK_ORDER, 10L);
            List<Notification> input = new ArrayList<>(List.of(n1, n2));
            when(notificationRepository.saveAll(anyList())).thenReturn(List.of(n2));
            // Throwing here prevents the expo SDK from making real network calls
            when(pushNotificationTokenService.findByUser(eq(2L)))
                    .thenThrow(new RuntimeException("prevent network call"));

            assertDoesNotThrow(() ->
                    notificationService.createMultiple(input, true, "Title"));

            verify(notificationRepository).saveAll(anyList());
            verify(messagingTemplate).convertAndSendToUser(eq("other@test.com"), eq("/notifications"), eq(n2));
            verify(pushNotificationTokenService).findByUser(eq(2L));
            verify(pushNotificationTokenService, never()).findByUser(eq(1L));
        }
    }

    // ── excludeCurrentUser (private, tested via reflection) ──────────────

    @Nested
    class ExcludeCurrentUser {

        private List<Notification> invoke(List<Notification> list) {
            return ReflectionTestUtils.invokeMethod(notificationService, "excludeCurrentUser", list);
        }

        @Test
        void noAuthentication_returnsInputListUnchanged() {
            SecurityContextHolder.clearContext();
            Notification n1 = notification(currentUser, "m", NotificationType.WORK_ORDER, 1L);
            List<Notification> input = List.of(n1);

            List<Notification> result = invoke(input);

            assertSame(input, result);
        }

        @Test
        void currentUserInList_filtersItOut() {
            Helper.setCurrentUser(currentUser);
            Notification n1 = notification(currentUser, "m1", NotificationType.WORK_ORDER, 1L);
            Notification n2 = notification(otherUser, "m2", NotificationType.WORK_ORDER, 2L);
            List<Notification> input = List.of(n1, n2);

            List<Notification> result = invoke(input);

            assertEquals(1, result.size());
            assertEquals(2L, result.get(0).getUser().getId());
        }

        @Test
        void currentUserNotInList_returnsAll() {
            Helper.setCurrentUser(currentUser);
            Notification n2 = notification(otherUser, "m", NotificationType.WORK_ORDER, 1L);
            List<Notification> input = List.of(n2);

            List<Notification> result = invoke(input);

            assertEquals(1, result.size());
            assertSame(n2, result.get(0));
        }

        @Test
        void allTargetingCurrentUser_returnsEmptyList() {
            Helper.setCurrentUser(currentUser);
            Notification n1 = notification(currentUser, "m", NotificationType.WORK_ORDER, 1L);

            List<Notification> result = invoke(List.of(n1));

            assertTrue(result.isEmpty());
        }

        @Test
        void authenticationWithNonCustomUserDetailPrincipal_returnsInputListUnchanged() {
            Authentication auth = new UsernamePasswordAuthenticationToken("anonymous", null);
            SecurityContextHolder.getContext().setAuthentication(auth);
            Notification n1 = notification(currentUser, "m", NotificationType.WORK_ORDER, 1L);
            List<Notification> input = List.of(n1);

            List<Notification> result = invoke(input);

            assertSame(input, result);
        }
    }
}
