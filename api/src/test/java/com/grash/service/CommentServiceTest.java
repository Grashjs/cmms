package com.grash.service;

import com.grash.dto.comment.CommentPatchDTO;
import com.grash.dto.comment.CommentPostDTO;
import com.grash.dto.comment.CommentShowDTO;
import com.grash.exception.CustomException;
import com.grash.factory.MailServiceFactory;
import com.grash.mapper.CommentMapper;
import com.grash.model.*;
import com.grash.model.enums.Language;
import com.grash.model.enums.NotificationType;
import com.grash.model.enums.webhook.WebhookEvent;
import com.grash.repository.CommentRepository;
import com.grash.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {
    @Mock CommentRepository repository;
    @Mock CommentMapper mapper;
    @Mock WorkOrderService workOrders;
    @Mock EntityManager em;
    @Mock ApplicationEventPublisher events;
    @Mock UserRepository users;
    @Mock NotificationService notifications;
    @Mock MessageSource messages;
    @Mock MailServiceFactory mailFactory;
    @Mock MailService mail;
    @Mock WebhookDispatchService webhooks;
    @InjectMocks CommentService service;
    User user;
    WorkOrder workOrder;

    @BeforeEach void setup() {
        Company company = new Company(); company.setId(52L);
        user = recipient(34L, company); user.setFirstName("Actor"); user.setLastName("Name");
        workOrder = new WorkOrder(); workOrder.setId(10L); workOrder.setCompany(company);
        workOrder.setTitle("Pump inspection");
        ReflectionTestUtils.setField(service, "frontendUrl", "https://atlas.test");
    }

    User recipient(long id, Company company) {
        User recipient = new User(); recipient.setId(id); recipient.setCompany(company);
        recipient.setEmail(id + "@atlas.test"); recipient.setEnabled(true);
        recipient.setLanguage(Language.FR); recipient.setUserSettings(new UserSettings());
        return recipient;
    }

    CommentPostDTO request(String content) {
        var request = new CommentPostDTO(); request.setWorkOrder(workOrder); request.setContent(content); return request;
    }

    void saves() {
        when(workOrders.checkAccessToWorkOrderId(10L, user)).thenReturn(workOrder);
        when(mapper.fromPostDto(any())).thenAnswer(call -> {
            CommentPostDTO request = call.getArgument(0);
            Comment result = new Comment(); result.setContent(request.getContent());
            result.setWorkOrder(request.getWorkOrder()); return result;
        });
        when(repository.saveAndFlush(any())).thenAnswer(call -> { Comment comment = call.getArgument(0); comment.setId(99L); return comment; });
        when(mapper.toShowDto(any())).thenReturn(new CommentShowDTO());
    }

    void notificationMessages() {
        when(messages.getMessage(anyString(), any(), eq(Locale.FRANCE)))
                .thenAnswer(call -> call.getArgument(0));
    }

    void recipients() {
        User assigned = recipient(42L, user.getCompany());
        User creator = recipient(43L, user.getCompany()); creator.setEnabled(false);
        User tagged = recipient(44L, user.getCompany()); tagged.getUserSettings().setEmailNotified(false);
        workOrder.setAssignedTo(List.of(user, assigned)); workOrder.setCreatedBy(43L);
        when(users.findByIdAndCompany_Id(43L, 52L)).thenReturn(Optional.of(creator));
        when(users.findByIdInAndCompany_Id(Set.of(42L, 44L), 52L)).thenReturn(List.of(assigned, tagged));
        notificationMessages();
        when(mailFactory.getMailService()).thenReturn(mail);
    }

    @Test void connectedCreationKeepsRecipientsMentionsEmailAndWebhookWithoutAnOfflineEvent() {
        saves();
        recipients();
        String content = "<b>Hello</b> @[Assigned](user:42) @[Tagged](user:44)";
        var result = service.create(request(content), user);
        var comment = result.getFirst();
        assertEquals("Hello @[Assigned](user:42) @[Tagged](user:44)", comment.getContent());
        assertSame(user, comment.getUser()); assertNull(comment.getSourceOperationId());
        verify(workOrders).checkAccessToWorkOrderId(10L, user);
        InOrder order = inOrder(repository, em, notifications, mail, webhooks);
        order.verify(repository).saveAndFlush(comment);
        order.verify(em).refresh(comment);
        order.verify(notifications).createMultiple(argThat(items ->
                items.stream().map(item -> item.getUser().getId()).toList().equals(List.of(42L, 43L, 44L))
                        && items.stream().allMatch(item -> item.getResourceId().equals(10L)
                        && item.getNotificationType() == NotificationType.WORK_ORDER
                        && item.getMessage().equals("notification_new_comment"))), eq(true), eq("new_comment"));
        order.verify(mail).sendMessageUsingThymeleafTemplate(aryEq(new String[]{"42@atlas.test"}), eq("new_comment"),
                argThat(variables -> variables.equals(Map.of(
                        "userFullName", "Actor Name", "workOrderTitle", "Pump inspection",
                        "commentContent", "Hello <a href=\"https://atlas.test/app/people-teams/people/42\">@Assigned</a> <a href=\"https://atlas.test/app/people-teams/people/44\">@Tagged</a>",
                        "commentLink", "https://atlas.test/app/work-orders/10?commentId=99"))),
                eq("new-comment.html"), eq(Locale.FRANCE));
        order.verify(webhooks).dispatchWebhook(eq(user.getCompany()), eq(WebhookEvent.NEW_COMMENT_ON_WORK_ORDER),
                eq(Map.of("commentId", 99L, "workOrderId", 10L, "workOrderTitle", "Pump inspection",
                        "commentContent", "Hello @Assigned @Tagged", "createdBy", "Actor Name")),
                eq("newComment"), same(result.getSecond()), isNull(), isNull(), isNull(), isNull(), isNull());
        verifyNoInteractions(events);
    }

    @Test void connectedCreationDoesNotAddTheOfflineEmptyContentRejection() {
        // The mocked repository only proves the service adds no offline rejection;
        // existing JPA @NotBlank validation is unchanged and may reject the save.
        saves(); notificationMessages();
        Comment comment = service.create(request("<p> </p>"), user).getFirst();
        assertEquals("", comment.getContent());
        verify(repository).saveAndFlush(comment);
        verify(notifications).createMultiple(anyList(), eq(true), eq("new_comment"));
        verifyNoInteractions(events);
    }

    Comment updating(String content) {
        Comment comment = new Comment(); comment.setId(99L); comment.setUser(user);
        comment.setWorkOrder(workOrder); comment.setContent(content);
        when(repository.findById(99L)).thenReturn(Optional.of(comment));
        when(mapper.updateComment(eq(comment), any())).thenAnswer(call -> {
            comment.setContent(((CommentPatchDTO) call.getArgument(1)).getContent()); return comment;
        });
        when(repository.saveAndFlush(comment)).thenReturn(comment);
        return comment;
    }

    @Test void connectedUpdateKeepsItsNotificationAndEmailPathWithoutCreationWebhookOrEvent() {
        Comment comment = updating("old"); recipients();
        CommentPatchDTO patch = new CommentPatchDTO();
        patch.setContent("<b>Changed</b> @[Assigned](user:42) @[Tagged](user:44)");
        assertSame(comment, service.update(99L, patch, user));
        assertEquals("Changed @[Assigned](user:42) @[Tagged](user:44)", comment.getContent());
        verify(em).refresh(comment);
        verify(notifications).createMultiple(argThat(items ->
                items.stream().map(item -> item.getUser().getId()).toList().equals(List.of(42L, 43L, 44L))),
                eq(true), eq("comment_updated"));
        verify(mail).sendMessageUsingThymeleafTemplate(aryEq(new String[]{"42@atlas.test"}), eq("comment_updated"),
                argThat(variables -> variables.get("commentContent").equals("Changed <a href=\"https://atlas.test/app/people-teams/people/42\">@Assigned</a> <a href=\"https://atlas.test/app/people-teams/people/44\">@Tagged</a>")),
                eq("comment-updated.html"), eq(Locale.FRANCE));
        verifyNoInteractions(events, webhooks);
    }

    @Test void connectedUpdateDoesNotAddTheOfflineEmptyContentRejection() {
        // As for creation, actual persistence keeps its historical entity validation.
        Comment comment = updating("old"); notificationMessages();
        CommentPatchDTO patch = new CommentPatchDTO(); patch.setContent("<p> </p>");
        assertEquals("", service.update(99L, patch, user).getContent());
        verify(repository).saveAndFlush(comment);
        verify(notifications).createMultiple(anyList(), eq(true), eq("comment_updated"));
        verifyNoInteractions(events, webhooks);
    }

    @Test void connectedCreateStillChecksWorkOrderAccessBeforeSavingOrNotifying() {
        when(mapper.fromPostDto(any())).thenReturn(new Comment());
        when(workOrders.checkAccessToWorkOrderId(10L, user))
                .thenThrow(new CustomException("Access denied", HttpStatus.FORBIDDEN));
        assertThrows(CustomException.class, () -> service.create(request("comment"), user));
        verifyNoInteractions(repository, em, notifications, mailFactory, webhooks, events);
    }

    @Test void connectedUpdateStillRejectsAnotherAuthorBeforeMappingOrSaving() {
        Comment comment = new Comment(); comment.setUser(recipient(35L, user.getCompany()));
        when(repository.findById(99L)).thenReturn(Optional.of(comment));
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(CustomException.class,
                () -> service.update(99L, new CommentPatchDTO(), user)).getHttpStatus());
        verify(repository, never()).saveAndFlush(any());
        verifyNoInteractions(mapper, em, notifications, mailFactory, webhooks, events);
    }

    @Test void offlineCreationUsesServerIdentityAndOnlyPublishesItsSpecificEvent() {
        saves();
        UUID operation = UUID.randomUUID();
        var offline = service.createOffline(request("<b>offline</b>"), user, operation).getFirst();
        assertEquals("offline", offline.getContent()); assertEquals(operation, offline.getSourceOperationId());
        assertSame(workOrder, offline.getWorkOrder()); assertSame(user, offline.getUser()); assertSame(user.getCompany(), offline.getCompany());
        verify(workOrders).checkAccessToWorkOrderId(10L, user);
        InOrder order = inOrder(repository, em, events);
        order.verify(repository).saveAndFlush(offline);
        order.verify(em).refresh(offline);
        order.verify(events).publishEvent(new CommentService.OfflineCommentCreated(99L, 52L, 34L));
        verifyNoInteractions(notifications, mailFactory, webhooks, users);
    }

    @Test void offlineBlankAfterCleaningCannotPersistOrScheduleAnEvent() {
        assertEquals("INVALID_COMMENT_CONTENT", assertThrows(CustomException.class,
                () -> service.createOffline(request("<p> </p>"), user, UUID.randomUUID())).getMessage());
        verifyNoInteractions(repository, mapper, events, workOrders);
    }

    @Test void textBeyondTenThousandCharactersRemainsValid() {
        saves();
        String content = "x".repeat(20_000);
        assertEquals(content, service.createOffline(request(content), user, UUID.randomUUID()).getFirst().getContent());
    }

    @Test void failedSaveDoesNotPublishAnEvent() {
        when(workOrders.checkAccessToWorkOrderId(10L, user)).thenReturn(workOrder);
        when(mapper.fromPostDto(any())).thenReturn(new Comment());
        when(repository.saveAndFlush(any())).thenThrow(new IllegalStateException("database failure"));
        assertThrows(IllegalStateException.class, () -> service.createOffline(request("comment"), user, UUID.randomUUID()));
        verifyNoInteractions(events, notifications, mailFactory, webhooks);
    }

    @Test void offlineCreationStillChecksCurrentAccessBeforeMappingOrSaving() {
        when(workOrders.checkAccessToWorkOrderId(10L, user))
                .thenThrow(new CustomException("Access denied", HttpStatus.FORBIDDEN));
        assertThrows(CustomException.class, () -> service.createOffline(request("comment"), user, UUID.randomUUID()));
        verifyNoInteractions(mapper, repository, em, events, notifications, mailFactory, webhooks);
    }
}
