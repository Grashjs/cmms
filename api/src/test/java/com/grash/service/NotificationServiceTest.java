package com.grash.service;

import com.grash.advancedsearch.FilterField;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.NotificationPatchDTO;
import com.grash.dto.PushTokenPayload;
import com.grash.exception.CustomException;
import com.grash.mapper.NotificationMapper;
import com.grash.model.Notification;
import com.grash.model.PushNotificationToken;
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
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
    private NotificationMapper notificationMapper;
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

    // ── create ───────────────────────────────────────────────────────────

    @Nested
    class Create {

        @Test
        void savesNotificationAndSendsToUser() {
            Notification n = notification(currentUser, "hello", NotificationType.INFO, 1L);
            when(notificationRepository.save(n)).thenReturn(n);

            notificationService.create(n);

            verify(notificationRepository).save(n);
            verify(messagingTemplate).convertAndSendToUser(eq("current@test.com"), eq("/notifications"), eq(n));
        }
    }

    // ── getAll ───────────────────────────────────────────────────────────

    @Nested
    class GetAll {

        @Test
        void client_returnsOnlyOwnNotifications() {
            Notification n = notification(currentUser, "m", NotificationType.INFO, 1L);
            List<Notification> own = List.of(n);
            when(notificationRepository.findByUser_Id(eq(1L))).thenReturn(own);

            Collection<Notification> result = notificationService.getAll(currentUser);

            assertSame(own, result);
            verify(notificationRepository, never()).findAll();
        }

        @Test
        void nonClient_returnsAllNotifications() {
            User admin = buildUser(3L, "admin@test.com");
            admin.getRole().setRoleType(RoleType.ROLE_SUPER_ADMIN);
            List<Notification> all = List.of(notification(otherUser, "m", NotificationType.INFO, 1L));
            when(notificationRepository.findAll()).thenReturn(all);

            Collection<Notification> result = notificationService.getAll(admin);

            assertSame(all, result);
            verify(notificationRepository, never()).findByUser_Id(anyLong());
        }
    }

    // ── getSearchCriteria ─────────────────────────────────────────────────

    @Nested
    class GetSearchCriteria {

        @Test
        void client_addsUserFilter() {
            SearchCriteria criteria = new SearchCriteria();

            SearchCriteria result = notificationService.getSearchCriteria(currentUser, criteria);

            assertSame(criteria, result);
            assertEquals(1, result.getFilterFields().size());
            FilterField field = result.getFilterFields().get(0);
            assertEquals("user", field.getField());
            assertEquals(1L, field.getValue());
            assertEquals("eq", field.getOperation());
        }

        @Test
        void client_appendsAdditionalFiltersWithoutReplacing() {
            SearchCriteria criteria = new SearchCriteria();

            notificationService.getSearchCriteria(currentUser, criteria);
            notificationService.getSearchCriteria(currentUser, criteria);

            assertEquals(2, criteria.getFilterFields().size());
        }

        @Test
        void nonClient_returnsCriteriaUnchanged() {
            User admin = buildUser(3L, "admin@test.com");
            admin.getRole().setRoleType(RoleType.ROLE_SUPER_ADMIN);
            SearchCriteria criteria = new SearchCriteria();

            SearchCriteria result = notificationService.getSearchCriteria(admin, criteria);

            assertSame(criteria, result);
            assertEquals(0, result.getFilterFields().size());
        }
    }

    // ── getById ──────────────────────────────────────────────────────────

    @Nested
    class GetById {

        private Notification storedNotification(Long id, User owner) {
            Notification n = notification(owner, "m", NotificationType.INFO, 1L);
            n.setId(id);
            return n;
        }

        @Test
        void foundAndOwned_returnsNotification() {
            Notification n = storedNotification(5L, currentUser);
            when(notificationRepository.findById(eq(5L))).thenReturn(Optional.of(n));

            Notification result = notificationService.getById(5L, currentUser);

            assertSame(n, result);
        }

        @Test
        void notFound_throwsNotFound() {
            when(notificationRepository.findById(eq(5L))).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> notificationService.getById(5L, currentUser));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
            assertEquals("Not found", ex.getMessage());
        }

        @Test
        void ownedByAnotherUser_throwsForbidden() {
            Notification n = storedNotification(5L, otherUser);
            when(notificationRepository.findById(eq(5L))).thenReturn(Optional.of(n));

            CustomException ex = assertThrows(CustomException.class,
                    () -> notificationService.getById(5L, currentUser));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }
    }

    // ── patch ────────────────────────────────────────────────────────────

    @Nested
    class Patch {

        private NotificationPatchDTO patchDto(boolean seen) {
            NotificationPatchDTO dto = new NotificationPatchDTO();
            dto.setSeen(seen);
            return dto;
        }

        @Test
        void foundAndOwned_updatesAndSaves() {
            Notification n = notification(currentUser, "m", NotificationType.INFO, 1L);
            n.setId(5L);
            n.setSeen(false);
            Notification updated = notification(currentUser, "m", NotificationType.INFO, 1L);
            updated.setId(5L);
            updated.setSeen(true);
            NotificationPatchDTO dto = patchDto(true);

            when(notificationRepository.findById(eq(5L))).thenReturn(Optional.of(n));
            when(notificationMapper.updateNotification(eq(n), eq(dto))).thenReturn(updated);
            when(notificationRepository.save(eq(updated))).thenReturn(updated);

            Notification result = notificationService.patch(5L, dto, currentUser);

            assertSame(updated, result);
            verify(notificationRepository).save(updated);
        }

        @Test
        void notFound_throwsNotFound() {
            NotificationPatchDTO dto = patchDto(true);
            when(notificationRepository.findById(eq(5L))).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> notificationService.patch(5L, dto, currentUser));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
            assertEquals("Notification not found", ex.getMessage());
        }

        @Test
        void ownedByAnotherUser_throwsForbidden() {
            Notification n = notification(otherUser, "m", NotificationType.INFO, 1L);
            n.setId(5L);
            NotificationPatchDTO dto = patchDto(true);
            when(notificationRepository.findById(eq(5L))).thenReturn(Optional.of(n));

            CustomException ex = assertThrows(CustomException.class,
                    () -> notificationService.patch(5L, dto, currentUser));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
            verify(notificationRepository, never()).save(any());
        }
    }

    // ── savePushToken ────────────────────────────────────────────────────

    @Nested
    class SavePushToken {

        private PushTokenPayload payload(String token) {
            PushTokenPayload payload = new PushTokenPayload();
            payload.setToken(token);
            return payload;
        }

        @Test
        void existingToken_isUpdatedAndSaved() {
            PushNotificationToken existing = PushNotificationToken.builder()
                    .id(1L)
                    .user(currentUser)
                    .token("old-token")
                    .build();
            when(pushNotificationTokenService.findByUser(eq(1L))).thenReturn(Optional.of(existing));
            ArgumentCaptor<PushNotificationToken> captor = ArgumentCaptor.forClass(PushNotificationToken.class);

            notificationService.savePushToken(currentUser, payload("new-token"));

            verify(pushNotificationTokenService).save(captor.capture());
            assertSame(existing, captor.getValue());
            assertEquals("new-token", captor.getValue().getToken());
            assertEquals(currentUser, captor.getValue().getUser());
        }

        @Test
        void noExistingToken_createsNewOne() {
            when(pushNotificationTokenService.findByUser(eq(1L))).thenReturn(Optional.empty());
            ArgumentCaptor<PushNotificationToken> captor = ArgumentCaptor.forClass(PushNotificationToken.class);

            notificationService.savePushToken(currentUser, payload("new-token"));

            verify(pushNotificationTokenService).save(captor.capture());
            assertEquals("new-token", captor.getValue().getToken());
            assertEquals(currentUser, captor.getValue().getUser());
        }
    }

    // ── delete ───────────────────────────────────────────────────────────

    @Nested
    class Delete {

        @Test
        void deletesById() {
            notificationService.delete(5L);

            verify(notificationRepository).deleteById(eq(5L));
        }
    }

    // ── findById ─────────────────────────────────────────────────────────

    @Nested
    class FindById {

        @Test
        void returnsNotificationWhenPresent() {
            Notification n = notification(currentUser, "m", NotificationType.INFO, 1L);
            n.setId(5L);
            when(notificationRepository.findById(eq(5L))).thenReturn(Optional.of(n));

            Optional<Notification> result = notificationService.findById(5L);

            assertTrue(result.isPresent());
            assertSame(n, result.get());
        }

        @Test
        void returnsEmptyWhenNotFound() {
            when(notificationRepository.findById(eq(5L))).thenReturn(Optional.empty());

            Optional<Notification> result = notificationService.findById(5L);

            assertTrue(result.isEmpty());
        }
    }

    // ── findByUser ───────────────────────────────────────────────────────

    @Nested
    class FindByUser {

        @Test
        void delegatesToRepository() {
            Notification n = notification(currentUser, "m", NotificationType.INFO, 1L);
            when(notificationRepository.findByUser_Id(eq(1L))).thenReturn(Collections.singletonList(n));

            Collection<Notification> result = notificationService.findByUser(1L);

            assertEquals(1, result.size());
            assertSame(n, result.iterator().next());
        }
    }

    // ── findBySearchCriteria ─────────────────────────────────────────────

    @Nested
    class FindBySearchCriteria {

        @Test
        void buildsSpecificationAndPage() {
            SearchCriteria criteria = new SearchCriteria();
            criteria.getFilterFields().add(FilterField.builder()
                    .field("user").value(1L).operation("eq").values(new ArrayList<>()).build());
            Notification n = notification(currentUser, "m", NotificationType.INFO, 1L);
            Page<Notification> page = new PageImpl<>(List.of(n));
            when(notificationRepository.findAll(nullable(Specification.class), any(Pageable.class))).thenReturn(page);

            Page<Notification> result = notificationService.findBySearchCriteria(criteria);

            assertSame(page, result);
            verify(notificationRepository).findAll(nullable(Specification.class), any(Pageable.class));
        }

        @Test
        void emptyCriteria_buildsWithEmptyFilterList() {
            SearchCriteria criteria = new SearchCriteria();
            Page<Notification> page = new PageImpl<>(Collections.emptyList());
            when(notificationRepository.findAll(nullable(Specification.class), any(Pageable.class))).thenReturn(page);

            Page<Notification> result = notificationService.findBySearchCriteria(criteria);

            assertEquals(0, result.getContent().size());
            verify(notificationRepository).findAll(nullable(Specification.class), any(Pageable.class));
        }
    }

    // ── readAll ──────────────────────────────────────────────────────────

    @Nested
    class ReadAll {

        @Test
        void marksAllAsSeenForUser() {
            notificationService.readAll(1L);

            verify(notificationRepository).readAll(eq(1L));
        }
    }
}
