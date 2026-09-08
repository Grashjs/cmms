package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.NotificationPatchDTO;
import com.grash.dto.PushTokenPayload;
import com.grash.dto.SuccessResponse;
import com.grash.model.Notification;
import com.grash.model.User;
import com.grash.security.CurrentUser;
import com.grash.service.NotificationService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.Collection;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "Operations on notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("")
    @PreAuthorize("permitAll()")
    public Collection<Notification> getAll(@Parameter(hidden = true) @CurrentUser User user) {
        return notificationService.getAll(user);
    }

    @PostMapping("/search")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<Notification>> search(@Parameter(description = "Notification search criteria") @RequestBody SearchCriteria searchCriteria,
                                                     @Parameter(hidden = true) @CurrentUser User user) {
        return ResponseEntity.ok(notificationService.findBySearchCriteria(notificationService.getSearchCriteria(user, searchCriteria)));
    }

    @GetMapping("/read-all")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public SuccessResponse readAll(@Parameter(hidden = true) @CurrentUser User user) {
        notificationService.readAll(user.getId());
        return new SuccessResponse(true, "Notifications read");
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public Notification getById(@PathVariable("id") Long id,
                                @Parameter(hidden = true) @CurrentUser User user) {
        return notificationService.getById(id, user);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public Notification patch(@Parameter(description = "Notification fields to update") @Valid @RequestBody NotificationPatchDTO notification,
                              @PathVariable("id") Long id,
                              @Parameter(hidden = true) @CurrentUser User user) {
        return notificationService.patch(id, notification, user);
    }

    @PostMapping("/push-token")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public SuccessResponse savePushToken(@Parameter(description = "Push notification token payload") @RequestBody @Valid PushTokenPayload tokenPayload,
                                         @Parameter(hidden = true) @CurrentUser User user) {
        notificationService.savePushToken(user, tokenPayload);
        return new SuccessResponse(true, "Ok");
    }
}