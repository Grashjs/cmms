package com.grash.controller;

import com.grash.dto.SuccessResponse;
import com.grash.dto.comment.CommentCriteria;
import com.grash.dto.comment.CommentPatchDTO;
import com.grash.dto.comment.CommentPostDTO;
import com.grash.dto.comment.CommentShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.CommentMapper;
import com.grash.mapper.UserMapper;
import com.grash.model.Comment;
import com.grash.model.User;
import com.grash.model.WorkOrderHistory;
import com.grash.security.CurrentUser;
import com.grash.service.CommentService;
import com.grash.service.WorkOrderHistoryService;
import com.grash.service.WorkOrderService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final CommentMapper commentMapper;
    private final WorkOrderService workOrderService;
    private final WorkOrderHistoryService workOrderHistoryService;
    private final UserMapper userMapper;

    @PostMapping("/search/{workOrderId}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public List<CommentShowDTO> search(@PathVariable Long workOrderId,
                                       @Parameter(hidden = true) @CurrentUser User user) {

        workOrderService.checkAccessToWorkOrderId(workOrderId, user);

        CommentCriteria criteria = new CommentCriteria();
        criteria.setWorkOrderId(workOrderId);

        Stream<CommentShowDTO> commentsStream =
                commentService.findByCriteria(criteria, user)
                        .stream()
                        .map(commentMapper::toShowDto);

        Stream<CommentShowDTO> historyStream =
                workOrderHistoryService.findByWorkOrder(workOrderId)
                        .stream()
                        .map(workOrderHistory -> {
                            CommentShowDTO dto = new CommentShowDTO();
                            dto.setId((long) (Math.random() * 99999999));
                            dto.setContent(workOrderHistory.getName());
                            dto.setSystem(true);
                            dto.setUser(userMapper.toMiniDto(workOrderHistory.getUser()));
                            dto.setCreatedAt(workOrderHistory.getCreatedAt().toInstant());
                            return dto;
                        });

        return Stream.concat(commentsStream, historyStream)
                .sorted(Comparator.comparing(CommentShowDTO::getCreatedAt).reversed())
                .toList();
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public CommentShowDTO create(@RequestBody @Valid CommentPostDTO comment,
                                 @Parameter(hidden = true) @CurrentUser User user) {
        return commentMapper.toShowDto(commentService.create(comment, user));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public CommentShowDTO getById(@PathVariable Long id, @Parameter(hidden = true) @CurrentUser User user) {

        Comment comment = commentService.findById(id).orElseThrow(() -> new CustomException("Not found",
                HttpStatus.NOT_FOUND));
        workOrderService.checkAccessToWorkOrderId(comment.getWorkOrder().getId(), user);
        return commentMapper.toShowDto(comment);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public CommentShowDTO update(@PathVariable Long id,
                                 @RequestBody @Valid CommentPatchDTO comment,
                                 @Parameter(hidden = true) @CurrentUser User user) {
        return commentMapper.toShowDto(commentService.update(id, comment, user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> delete(@PathVariable("id") Long id,
                                                  @Parameter(hidden = true) @CurrentUser User user) {

        Comment savedComment =
                commentService.findById(id).orElseThrow(() -> new CustomException("Not found",
                        HttpStatus.NOT_FOUND));
        if (!savedComment.getUser().getId().equals(user.getId()))
            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        commentService.delete(id);
        return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"), HttpStatus.OK);
    }
}
