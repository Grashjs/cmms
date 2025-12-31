package com.grash.service;

import com.grash.dto.WebSocketMessageDTO;
import com.grash.dto.WorkOrderMessageShowDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyNewMessage(Long workOrderId, WorkOrderMessageShowDTO message) {
        WebSocketMessageDTO notification = WebSocketMessageDTO.newMessage(workOrderId, message);
        messagingTemplate.convertAndSend("/topic/work-order/" + workOrderId + "/messages", notification);
    }

    public void notifyMessageUpdated(Long workOrderId, WorkOrderMessageShowDTO message) {
        WebSocketMessageDTO notification = WebSocketMessageDTO.messageUpdated(workOrderId, message);
        messagingTemplate.convertAndSend("/topic/work-order/" + workOrderId + "/messages", notification);
    }

    public void notifyMessageDeleted(Long workOrderId, Long messageId) {
        WebSocketMessageDTO notification = WebSocketMessageDTO.messageDeleted(workOrderId, messageId);
        messagingTemplate.convertAndSend("/topic/work-order/" + workOrderId + "/messages", notification);
    }

    public void notifyMessageRead(Long workOrderId, Long messageId, Long userId) {
        WebSocketMessageDTO notification = WebSocketMessageDTO.messageRead(workOrderId, messageId, userId);
        messagingTemplate.convertAndSend("/topic/work-order/" + workOrderId + "/messages", notification);
    }

    public void notifyReactionToggled(Long workOrderId, Long messageId, Long userId, String reaction) {
        WebSocketMessageDTO notification = WebSocketMessageDTO.reactionToggled(workOrderId, messageId, userId, reaction);
        messagingTemplate.convertAndSend("/topic/work-order/" + workOrderId + "/messages", notification);
    }

    public void notifyTyping(Long workOrderId, Long userId, String userName, boolean isTyping) {
        messagingTemplate.convertAndSend("/topic/work-order/" + workOrderId + "/typing",
                new TypingNotification(userId, userName, isTyping));
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class TypingNotification {
        private Long userId;
        private String userName;
        private boolean isTyping;
    }
}
