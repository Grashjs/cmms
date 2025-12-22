package com.grash.controller;

import com.grash.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final WebSocketNotificationService webSocketNotificationService;

    @MessageMapping("/work-order/{workOrderId}/typing")
    public void handleTyping(@DestinationVariable Long workOrderId, @Payload TypingMessage message) {
        webSocketNotificationService.notifyTyping(
                workOrderId,
                message.getUserId(),
                message.getUserName(),
                message.isTyping()
        );
    }

    @lombok.Data
    public static class TypingMessage {
        private Long userId;
        private String userName;
        private boolean typing;
    }
}
