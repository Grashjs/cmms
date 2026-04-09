package com.grash.service;

import com.grash.dto.comment.CommentCriteria;
import com.grash.dto.comment.CommentPatchDTO;
import com.grash.dto.comment.CommentPostDTO;
import com.grash.exception.CustomException;
import com.grash.factory.MailServiceFactory;
import com.grash.mapper.CommentMapper;
import com.grash.model.*;
import com.grash.model.enums.NotificationType;
import com.grash.repository.CommentRepository;
import com.grash.repository.UserRepository;
import com.grash.utils.Helper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final WorkOrderService workOrderService;
    private final EntityManager em;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final MessageSource messageSource;
    private final MailServiceFactory mailServiceFactory;

    @Value("${frontend.url}")
    private String frontendUrl;

    public Comment create(@Valid CommentPostDTO commentReq, User user) {
        Comment comment = commentMapper.fromPostDto(commentReq);
        WorkOrder workOrder = workOrderService.checkAccessToWorkOrderId(commentReq.getWorkOrder().getId(), user);

        comment.setUser(user);
        Comment savedComment = commentRepository.saveAndFlush(comment);
        em.refresh(savedComment);

        Set<User> notifiedUsers = getNotifiedUsers(savedComment, workOrder, user);
        sendCommentNotifications(savedComment, workOrder, notifiedUsers, user, false);

        return savedComment;
    }


    public List<Comment> getAll() {
        return commentRepository.findAll();
    }

    public void delete(Long id) {
        commentRepository.deleteById(id);
    }

    public Optional<Comment> findById(Long id) {
        return commentRepository.findById(id);
    }

    public Comment update(Long id, CommentPatchDTO commentPatchDTO, User user) {
        Comment savedComment =
                commentRepository.findById(id).orElseThrow(() -> new CustomException("Not found",
                        HttpStatus.NOT_FOUND));
        if (!savedComment.getUser().getId().equals(user.getId()))
            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);

        WorkOrder workOrder = savedComment.getWorkOrder();

        Comment updatedComment = commentRepository.saveAndFlush(commentMapper.updateComment(savedComment,
                commentPatchDTO));
        em.refresh(updatedComment);

        Set<User> notifiedUsers = getNotifiedUsers(updatedComment, workOrder, user);
        sendCommentNotifications(updatedComment, workOrder, notifiedUsers, user, true);

        return updatedComment;
    }

    public List<Comment> findByCriteria(CommentCriteria criteria, User user) {
        Specification<Comment> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get(Comment_.company).get("id"), user.getCompany().getId()));
            predicates.add(cb.equal(root.get(Comment_.workOrder).get("id"), criteria.getWorkOrderId()));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return commentRepository.findAll(specification);
    }

    public long countByWorkOrderId(Long workOrderId, User user) {
        workOrderService.checkAccessToWorkOrderId(workOrderId, user);
        return commentRepository.countByWorkOrderId(workOrderId);
    }

    private Set<User> getNotifiedUsers(Comment comment, WorkOrder workOrder, User user) {
        Stream<User> workOrderUsers = workOrder.getUsers().stream();
        Stream<User> creatorStream = workOrder.getCreatedBy() == null
                ? Stream.empty()
                : userRepository.findByIdAndCompany_Id(workOrder.getCreatedBy(), user.getCompany().getId()).stream();
        Stream<User> taggedUsers = userRepository
                .findByIdInAndCompany_Id(comment.extractTaggedUserIds(), user.getCompany().getId())
                .stream();

        return Stream.of(workOrderUsers, creatorStream, taggedUsers)
                .flatMap(s -> s)
                .filter(u -> !u.getId().equals(user.getId()))
                .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(User::getId))));
    }

    private String formatCommentContent(String content) {
        // Convert @[name](user:id) mentions to clickable links
        return content.replaceAll("@\\[(.*?)\\]\\(user:(\\d+)\\)",
                "<a href=\"" + frontendUrl + "/app/people-teams/people/$2\">@$1</a>");
    }

    @Async
    public void sendCommentNotifications(Comment comment, WorkOrder workOrder, Set<User> notifiedUsers, User actor,
                                         boolean isUpdate) {
        Locale locale = Helper.getLocale(actor);
        String notificationKey = isUpdate ? "notification_comment_updated" : "notification_new_comment";
        String emailTitleKey = isUpdate ? "comment_updated" : "new_comment";
        String emailTemplate = isUpdate ? "comment-updated.html" : "new-comment.html";

        String message = messageSource.getMessage(notificationKey,
                new String[]{actor.getFullName(), workOrder.getTitle()}, locale);

        List<Notification> notifications = notifiedUsers.stream()
                .map(notifiedUser -> new Notification(message, notifiedUser, NotificationType.WORK_ORDER,
                        workOrder.getId()))
                .toList();

        notificationService.createMultiple(notifications, true,
                messageSource.getMessage(emailTitleKey, null, locale));

        sendCommentEmail(comment, workOrder, notifiedUsers, actor, locale, emailTitleKey, emailTemplate);
    }

    private void sendCommentEmail(Comment comment, WorkOrder workOrder, Set<User> notifiedUsers, User actor,
                                  Locale locale, String emailTitleKey, String emailTemplate) {
        String commentContent = formatCommentContent(comment.getContent());
        String commentLink =
                frontendUrl + "/app/work-orders/" + workOrder.getId() + "?commentId=" + comment.getId();

        Map<String, Object> mailVariables = new HashMap<>();
        mailVariables.put("userFullName", actor.getFullName());
        mailVariables.put("workOrderTitle", workOrder.getTitle());
        mailVariables.put("commentContent", commentContent);
        mailVariables.put("commentLink", commentLink);

        Collection<User> usersToMail = notifiedUsers.stream()
                .filter(u -> u.isEnabled() && u.getUserSettings() != null
                        && u.getUserSettings().shouldEmailUpdatesForWorkOrders())
                .collect(Collectors.toList());

        if (!usersToMail.isEmpty()) {
            mailServiceFactory.getMailService().sendMessageUsingThymeleafTemplate(
                    usersToMail.stream().map(User::getEmail).toArray(String[]::new),
                    messageSource.getMessage(emailTitleKey, null, locale),
                    mailVariables,
                    emailTemplate,
                    Helper.getLocale(usersToMail.stream().findFirst().get())
            );
        }
    }
}
