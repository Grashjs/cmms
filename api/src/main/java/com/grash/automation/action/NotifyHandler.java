package com.grash.automation.action;

import com.grash.automation.eval.ExecutionContext;
import com.grash.automation.model.ActionType;
import com.grash.automation.model.AutomationActionStep;
import com.grash.exception.CustomException;
import com.grash.model.Notification;
import com.grash.model.Team;
import com.grash.model.User;
import com.grash.model.enums.NotificationType;
import com.grash.service.NotificationService;
import com.grash.service.TeamService;
import com.grash.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Notifies a team or a single user, in-app and by push.
 *
 * <p>Parameters: {@code message} (required, may interpolate placeholders) and exactly one of
 * {@code team} or {@code user} (ids).
 *
 * <p>{@code Notification} extends {@code Audit} and has no company column, so the obligation
 * every other handler carries — setting the company by hand — does not apply here. Worth stating
 * rather than leaving the reader to wonder why this one is different.
 */
@Component
@RequiredArgsConstructor
public class NotifyHandler implements ActionHandler {

    private final NotificationService notificationService;
    private final TeamService teamService;
    private final UserService userService;

    @Override
    public ActionType getType() {
        return ActionType.NOTIFY;
    }

    @Override
    public ActionDescriptor descriptor() {
        // Neither recipient is marked required, because exactly one of them is — a constraint a
        // flat parameter list cannot express. The editor offers a choice between the two; the
        // handler enforces it, and says so in the run log if it is violated.
        return new ActionDescriptor(ActionType.NOTIFY, "automation_action_notify",
                List.of(
                        ActionDescriptor.Parameter.text("message", true),
                        ActionDescriptor.Parameter.entity("team", "TEAM", false),
                        ActionDescriptor.Parameter.entity("user", "USER", false)));
    }

    @Override
    public void execute(AutomationActionStep step, ExecutionContext context) {
        ActionParameters parameters = ActionParameters.of(step.getParameters(), context);
        String message = parameters.requireString("message");

        List<User> recipients = recipients(parameters, context);
        if (recipients.isEmpty()) {
            // Not silently successful: an empty team is a configuration mistake, and a rule that
            // reports success while notifying nobody is the kind of thing nobody notices for
            // months.
            throw new CustomException("No recipients for this notification", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        List<Notification> notifications = new ArrayList<>();
        for (User recipient : recipients) {
            Notification notification = new Notification();
            notification.setUser(recipient);
            notification.setMessage(message);
            notification.setNotificationType(notificationType(context));
            notification.setResourceId(context.getEvent().entityId());
            notifications.add(notification);
        }
        notificationService.createMultiple(notifications, true, message);
    }

    private NotificationType notificationType(ExecutionContext context) {
        return switch (context.getEvent().entityType()) {
            case ASSET -> NotificationType.ASSET;
            case WORK_ORDER, TASK -> NotificationType.WORK_ORDER;
            case REQUEST -> NotificationType.REQUEST;
            case PURCHASE_ORDER -> NotificationType.PURCHASE_ORDER;
            case PART -> NotificationType.PART;
        };
    }

    private List<User> recipients(ActionParameters parameters, ExecutionContext context) {
        Long teamId = parameters.getLong("team");
        Long userId = parameters.getLong("user");
        if ((teamId == null) == (userId == null)) {
            throw new CustomException("Notification needs exactly one of \"team\" or \"user\"",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (teamId != null) {
            Team team = teamService.findById(teamId)
                    .orElseThrow(() -> new CustomException("Team " + teamId + " not found", HttpStatus.NOT_FOUND));
            assertSameCompany(team.getCompany().getId(), context, "Team " + teamId);
            return new ArrayList<>(team.getUsers());
        }
        User user = userService.findById(userId)
                .orElseThrow(() -> new CustomException("User " + userId + " not found", HttpStatus.NOT_FOUND));
        assertSameCompany(user.getCompany().getId(), context, "User " + userId);
        return List.of(user);
    }

    private void assertSameCompany(Long companyId, ExecutionContext context, String what) {
        if (!companyId.equals(context.getCompany().getId())) {
            throw new CustomException(what + " belongs to another company", HttpStatus.FORBIDDEN);
        }
    }
}
