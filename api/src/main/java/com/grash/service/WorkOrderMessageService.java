package com.grash.service;

import com.grash.dto.UserMiniDTO;
import com.grash.dto.WorkOrderMessagePatchDTO;
import com.grash.dto.WorkOrderMessagePostDTO;
import com.grash.dto.WorkOrderMessageReactionDTO;
import com.grash.dto.WorkOrderMessageShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.UserMapper;
import com.grash.mapper.WorkOrderMessageMapper;
import com.grash.model.*;
import com.grash.model.enums.MessageType;
import com.grash.repository.WorkOrderMessageReactionRepository;
import com.grash.repository.WorkOrderMessageReadRepository;
import com.grash.repository.WorkOrderMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkOrderMessageService {

    private final WorkOrderMessageRepository workOrderMessageRepository;
    private final WorkOrderMessageReadRepository workOrderMessageReadRepository;
    private final WorkOrderMessageReactionRepository workOrderMessageReactionRepository;
    private final WorkOrderService workOrderService;
    private final UserService userService;
    private final FileService fileService;
    private final WorkOrderMessageMapper workOrderMessageMapper;
    private final UserMapper userMapper;
    private final WebSocketNotificationService webSocketNotificationService;

    public Optional<WorkOrderMessage> findById(Long id) {
        return workOrderMessageRepository.findById(id);
    }

    @Transactional
    public WorkOrderMessage create(WorkOrderMessagePostDTO dto) {
        OwnUser currentUser = userService.getCurrentUser();
        WorkOrder workOrder = workOrderService.findById(dto.getWorkOrderId())
                .orElseThrow(() -> new CustomException("Work Order not found", HttpStatus.NOT_FOUND));

        // Validate user has access to this work order
        if (!canAccessWorkOrder(currentUser, workOrder)) {
            throw new CustomException("You don't have permission to access this Work Order", HttpStatus.FORBIDDEN);
        }

        // Validate file if provided
        File file = null;
        if (dto.getFileId() != null) {
            file = fileService.findById(dto.getFileId())
                    .orElseThrow(() -> new CustomException("File not found", HttpStatus.NOT_FOUND));
        }

        // Validate parent message if provided
        WorkOrderMessage parentMessage = null;
        if (dto.getParentMessageId() != null) {
            parentMessage = findById(dto.getParentMessageId())
                    .orElseThrow(() -> new CustomException("Parent message not found", HttpStatus.NOT_FOUND));
        }

        WorkOrderMessage message = new WorkOrderMessage(
                workOrder,
                currentUser,
                dto.getMessageType(),
                dto.getContent(),
                file
        );
        message.setParentMessage(parentMessage);

        WorkOrderMessage savedMessage = workOrderMessageRepository.save(message);
        
        // Send WebSocket notification
        WorkOrderMessageShowDTO messageDTO = enrichMessageDTO(savedMessage, currentUser);
        webSocketNotificationService.notifyNewMessage(workOrder.getId(), messageDTO);
        
        return savedMessage;
    }

    @Transactional
    public WorkOrderMessage update(Long id, WorkOrderMessagePatchDTO dto) {
        OwnUser currentUser = userService.getCurrentUser();
        WorkOrderMessage message = findById(id)
                .orElseThrow(() -> new CustomException("Message not found", HttpStatus.NOT_FOUND));

        // Only message author can edit
        if (!message.getUser().getId().equals(currentUser.getId())) {
            throw new CustomException("You can only edit your own messages", HttpStatus.FORBIDDEN);
        }

        // Update content if provided
        if (dto.getContent() != null && !dto.getContent().equals(message.getContent())) {
            message.setContent(dto.getContent());
            message.setEdited(true);
        }

        // Handle deletion
        if (dto.isDeleted()) {
            message.setDeleted(true);
            WorkOrderMessage updatedMessage = workOrderMessageRepository.save(message);
            webSocketNotificationService.notifyMessageDeleted(message.getWorkOrder().getId(), message.getId());
            return updatedMessage;
        }

        WorkOrderMessage updatedMessage = workOrderMessageRepository.save(message);
        
        // Send WebSocket notification
        WorkOrderMessageShowDTO messageDTO = enrichMessageDTO(updatedMessage, currentUser);
        webSocketNotificationService.notifyMessageUpdated(updatedMessage.getWorkOrder().getId(), messageDTO);
        
        return updatedMessage;
    }

    public List<WorkOrderMessage> getMessagesByWorkOrder(Long workOrderId) {
        WorkOrder workOrder = workOrderService.findById(workOrderId)
                .orElseThrow(() -> new CustomException("Work Order not found", HttpStatus.NOT_FOUND));

        OwnUser currentUser = userService.getCurrentUser();
        if (!canAccessWorkOrder(currentUser, workOrder)) {
            throw new CustomException("You don't have permission to access this Work Order", HttpStatus.FORBIDDEN);
        }

        return workOrderMessageRepository.findActiveMessagesByWorkOrder(workOrder);
    }

    public List<WorkOrderMessageShowDTO> getMessagesWithDetails(Long workOrderId) {
        List<WorkOrderMessage> messages = getMessagesByWorkOrder(workOrderId);
        OwnUser currentUser = userService.getCurrentUser();

        return messages.stream()
                .map(message -> enrichMessageDTO(message, currentUser))
                .collect(Collectors.toList());
    }

    private WorkOrderMessageShowDTO enrichMessageDTO(WorkOrderMessage message, OwnUser currentUser) {
        WorkOrderMessageShowDTO dto = workOrderMessageMapper.toShowDto(message);

        // Add reactions
        dto.setReactions(getReactionsForMessage(message, currentUser));

        // Add read by users
        dto.setReadBy(getUsersWhoRead(message));

        // Check if current user has read
        dto.setReadByCurrentUser(hasUserRead(message, currentUser));

        return dto;
    }

    private List<WorkOrderMessageReactionDTO> getReactionsForMessage(WorkOrderMessage message, OwnUser currentUser) {
        List<WorkOrderMessageReaction> reactions = workOrderMessageReactionRepository.findByMessage(message);

        // Group by reaction type
        Map<String, List<WorkOrderMessageReaction>> groupedReactions = reactions.stream()
                .collect(Collectors.groupingBy(WorkOrderMessageReaction::getReaction));

        return groupedReactions.entrySet().stream()
                .map(entry -> {
                    String reactionType = entry.getKey();
                    List<WorkOrderMessageReaction> reactionList = entry.getValue();
                    List<UserMiniDTO> users = reactionList.stream()
                            .map(r -> userMapper.toMiniDto(r.getUser()))
                            .collect(Collectors.toList());
                    boolean currentUserReacted = reactionList.stream()
                            .anyMatch(r -> r.getUser().getId().equals(currentUser.getId()));

                    return new WorkOrderMessageReactionDTO(reactionType, reactionList.size(), users, currentUserReacted);
                })
                .collect(Collectors.toList());
    }

    private List<UserMiniDTO> getUsersWhoRead(WorkOrderMessage message) {
        return message.getReads().stream()
                .map(read -> userMapper.toMiniDto(read.getUser()))
                .collect(Collectors.toList());
    }

    private boolean hasUserRead(WorkOrderMessage message, OwnUser user) {
        return workOrderMessageReadRepository.existsByMessageAndUser(message, user);
    }

    @Transactional
    public void markAsRead(Long messageId) {
        OwnUser currentUser = userService.getCurrentUser();
        WorkOrderMessage message = findById(messageId)
                .orElseThrow(() -> new CustomException("Message not found", HttpStatus.NOT_FOUND));

        // Don't mark own messages as read
        if (message.getUser().getId().equals(currentUser.getId())) {
            return;
        }

        // Check if already read
        if (!workOrderMessageReadRepository.existsByMessageAndUser(message, currentUser)) {
            WorkOrderMessageRead read = new WorkOrderMessageRead(message, currentUser);
            workOrderMessageReadRepository.save(read);
            
            // Send WebSocket notification
            webSocketNotificationService.notifyMessageRead(message.getWorkOrder().getId(), messageId, currentUser.getId());
        }
    }

    @Transactional
    public void markAllAsRead(Long workOrderId) {
        List<WorkOrderMessage> messages = getMessagesByWorkOrder(workOrderId);
        OwnUser currentUser = userService.getCurrentUser();

        messages.forEach(message -> {
            if (!message.getUser().getId().equals(currentUser.getId()) &&
                    !workOrderMessageReadRepository.existsByMessageAndUser(message, currentUser)) {
                WorkOrderMessageRead read = new WorkOrderMessageRead(message, currentUser);
                workOrderMessageReadRepository.save(read);
            }
        });
    }

    public long getUnreadCount(Long workOrderId) {
        WorkOrder workOrder = workOrderService.findById(workOrderId)
                .orElseThrow(() -> new CustomException("Work Order not found", HttpStatus.NOT_FOUND));

        OwnUser currentUser = userService.getCurrentUser();
        return workOrderMessageRepository.countUnreadMessages(workOrder, currentUser.getId());
    }

    @Transactional
    public void toggleReaction(Long messageId, String reaction) {
        OwnUser currentUser = userService.getCurrentUser();
        WorkOrderMessage message = findById(messageId)
                .orElseThrow(() -> new CustomException("Message not found", HttpStatus.NOT_FOUND));

        Optional<WorkOrderMessageReaction> existing = workOrderMessageReactionRepository
                .findByMessageAndUserAndReaction(message, currentUser, reaction);

        if (existing.isPresent()) {
            // Remove reaction
            workOrderMessageReactionRepository.delete(existing.get());
        } else {
            // Add reaction
            WorkOrderMessageReaction newReaction = new WorkOrderMessageReaction(message, currentUser, reaction);
            workOrderMessageReactionRepository.save(newReaction);
        }
        
        // Send WebSocket notification
        webSocketNotificationService.notifyReactionToggled(message.getWorkOrder().getId(), messageId, currentUser.getId(), reaction);
    }

    @Transactional
    public WorkOrderMessage createSystemMessage(WorkOrder workOrder, String content) {
        WorkOrderMessage message = new WorkOrderMessage(
                workOrder,
                null, // System messages have no user
                MessageType.SYSTEM,
                content,
                null
        );
        return workOrderMessageRepository.save(message);
    }

    private boolean canAccessWorkOrder(OwnUser user, WorkOrder workOrder) {
        // User can access if:
        // 1. They are assigned to the work order
        // 2. They have permission to view all work orders
        // 3. They are in a team assigned to the work order
        // 4. They created the work order

        return workOrder.isAssignedTo(user) ||
                user.getRole().getViewOtherPermissions().contains(com.grash.model.enums.PermissionEntity.WORK_ORDERS) ||
                (workOrder.getCreatedBy() != null && workOrder.getCreatedBy().equals(user.getId()));
    }

    public boolean isWorkOrderCompleted(Long workOrderId) {
        WorkOrder workOrder = workOrderService.findById(workOrderId)
                .orElseThrow(() -> new CustomException("Work Order not found", HttpStatus.NOT_FOUND));
        return workOrder.getStatus() == com.grash.model.enums.Status.COMPLETE;
    }
}
