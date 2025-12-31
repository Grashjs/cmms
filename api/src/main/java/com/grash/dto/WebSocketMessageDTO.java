package com.grash.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessageDTO {

    private String type; // NEW_MESSAGE, MESSAGE_UPDATED, MESSAGE_DELETED, MESSAGE_READ, REACTION_ADDED, REACTION_REMOVED

    private Long workOrderId;

    private WorkOrderMessageShowDTO message;

    private Long messageId;

    private Long userId;

    private String reaction;

    public static WebSocketMessageDTO newMessage(Long workOrderId, WorkOrderMessageShowDTO message) {
        WebSocketMessageDTO dto = new WebSocketMessageDTO();
        dto.setType("NEW_MESSAGE");
        dto.setWorkOrderId(workOrderId);
        dto.setMessage(message);
        return dto;
    }

    public static WebSocketMessageDTO messageUpdated(Long workOrderId, WorkOrderMessageShowDTO message) {
        WebSocketMessageDTO dto = new WebSocketMessageDTO();
        dto.setType("MESSAGE_UPDATED");
        dto.setWorkOrderId(workOrderId);
        dto.setMessage(message);
        return dto;
    }

    public static WebSocketMessageDTO messageDeleted(Long workOrderId, Long messageId) {
        WebSocketMessageDTO dto = new WebSocketMessageDTO();
        dto.setType("MESSAGE_DELETED");
        dto.setWorkOrderId(workOrderId);
        dto.setMessageId(messageId);
        return dto;
    }

    public static WebSocketMessageDTO messageRead(Long workOrderId, Long messageId, Long userId) {
        WebSocketMessageDTO dto = new WebSocketMessageDTO();
        dto.setType("MESSAGE_READ");
        dto.setWorkOrderId(workOrderId);
        dto.setMessageId(messageId);
        dto.setUserId(userId);
        return dto;
    }

    public static WebSocketMessageDTO reactionToggled(Long workOrderId, Long messageId, Long userId, String reaction) {
        WebSocketMessageDTO dto = new WebSocketMessageDTO();
        dto.setType("REACTION_TOGGLED");
        dto.setWorkOrderId(workOrderId);
        dto.setMessageId(messageId);
        dto.setUserId(userId);
        dto.setReaction(reaction);
        return dto;
    }
}
