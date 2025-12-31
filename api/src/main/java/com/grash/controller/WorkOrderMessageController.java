package com.grash.controller;

import com.grash.dto.SuccessResponse;
import com.grash.dto.WorkOrderMessagePatchDTO;
import com.grash.dto.WorkOrderMessagePostDTO;
import com.grash.dto.WorkOrderMessageShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.WorkOrderMessageMapper;
import com.grash.model.OwnUser;
import com.grash.model.WorkOrderMessage;
import com.grash.service.UserService;
import com.grash.service.WorkOrderMessageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/work-order-messages")
@Api(tags = "workOrderMessage")
@RequiredArgsConstructor
public class WorkOrderMessageController {

    private final WorkOrderMessageService workOrderMessageService;
    private final WorkOrderMessageMapper workOrderMessageMapper;
    private final UserService userService;

    @GetMapping("/work-order/{workOrderId}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully retrieved messages"),
            @ApiResponse(code = 403, message = "You don't have permission to access this Work Order"),
            @ApiResponse(code = 404, message = "Work Order not found")
    })
    public ResponseEntity<List<WorkOrderMessageShowDTO>> getMessagesByWorkOrder(
            @ApiParam("Work Order ID") @PathVariable("workOrderId") Long workOrderId) {
        List<WorkOrderMessageShowDTO> messages = workOrderMessageService.getMessagesWithDetails(workOrderId);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Message created successfully"),
            @ApiResponse(code = 400, message = "Invalid message data"),
            @ApiResponse(code = 403, message = "You don't have permission to send messages to this Work Order"),
            @ApiResponse(code = 404, message = "Work Order not found")
    })
    public ResponseEntity<WorkOrderMessageShowDTO> createMessage(
            @ApiParam("Message data") @Valid @RequestBody WorkOrderMessagePostDTO messageDTO) {
        
        // Check if work order is completed (read-only mode)
        if (workOrderMessageService.isWorkOrderCompleted(messageDTO.getWorkOrderId())) {
            throw new CustomException("Cannot send messages to a completed Work Order", HttpStatus.FORBIDDEN);
        }

        WorkOrderMessage message = workOrderMessageService.create(messageDTO);
        OwnUser currentUser = userService.getCurrentUser();
        WorkOrderMessageShowDTO responseDTO = workOrderMessageMapper.toShowDto(message);
        responseDTO.setReactions(List.of());
        responseDTO.setReadBy(List.of());
        responseDTO.setReadByCurrentUser(false);
        
        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Message updated successfully"),
            @ApiResponse(code = 403, message = "You can only edit your own messages"),
            @ApiResponse(code = 404, message = "Message not found")
    })
    public ResponseEntity<WorkOrderMessageShowDTO> updateMessage(
            @ApiParam("Message ID") @PathVariable("id") Long id,
            @ApiParam("Updated message data") @Valid @RequestBody WorkOrderMessagePatchDTO messageDTO) {
        
        WorkOrderMessage message = workOrderMessageService.update(id, messageDTO);
        OwnUser currentUser = userService.getCurrentUser();
        WorkOrderMessageShowDTO responseDTO = workOrderMessageMapper.toShowDto(message);
        
        return ResponseEntity.ok(responseDTO);
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Message marked as read"),
            @ApiResponse(code = 404, message = "Message not found")
    })
    public ResponseEntity<SuccessResponse> markAsRead(
            @ApiParam("Message ID") @PathVariable("id") Long id) {
        workOrderMessageService.markAsRead(id);
        return ResponseEntity.ok(new SuccessResponse(true, "Message marked as read"));
    }

    @PostMapping("/work-order/{workOrderId}/read-all")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "All messages marked as read"),
            @ApiResponse(code = 403, message = "You don't have permission to access this Work Order"),
            @ApiResponse(code = 404, message = "Work Order not found")
    })
    public ResponseEntity<SuccessResponse> markAllAsRead(
            @ApiParam("Work Order ID") @PathVariable("workOrderId") Long workOrderId) {
        workOrderMessageService.markAllAsRead(workOrderId);
        return ResponseEntity.ok(new SuccessResponse(true, "All messages marked as read"));
    }

    @GetMapping("/work-order/{workOrderId}/unread-count")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully retrieved unread count"),
            @ApiResponse(code = 403, message = "You don't have permission to access this Work Order"),
            @ApiResponse(code = 404, message = "Work Order not found")
    })
    public ResponseEntity<Long> getUnreadCount(
            @ApiParam("Work Order ID") @PathVariable("workOrderId") Long workOrderId) {
        long count = workOrderMessageService.getUnreadCount(workOrderId);
        return ResponseEntity.ok(count);
    }

    @PostMapping("/{id}/reaction")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Reaction toggled successfully"),
            @ApiResponse(code = 404, message = "Message not found")
    })
    public ResponseEntity<SuccessResponse> toggleReaction(
            @ApiParam("Message ID") @PathVariable("id") Long id,
            @ApiParam("Reaction emoji") @RequestParam String reaction) {
        workOrderMessageService.toggleReaction(id, reaction);
        return ResponseEntity.ok(new SuccessResponse(true, "Reaction toggled"));
    }
}
