package com.grash.controller;

import com.grash.advancedsearch.FilterField;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.NotificationPatchDTO;
import com.grash.dto.PushTokenPayload;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.Notification;
import com.grash.model.OwnUser;
import com.grash.model.PushNotificationToken;
import com.grash.model.enums.RoleType;
import com.grash.service.NotificationService;
import com.grash.service.PushNotificationTokenService;
import com.grash.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

@RestController
@RequestMapping("/notifications")
@Api(tags = "notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;
    private final PushNotificationTokenService pushNotificationTokenService;

    @GetMapping("")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "NotificationCategory not found")})
    public Collection<Notification> getAll(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            return notificationService.findByUser(user.getId());
        } else return notificationService.getAll();
    }

    @PostMapping("/search")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<Notification>> search(@RequestBody SearchCriteria searchCriteria, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            searchCriteria.getFilterFields().add(FilterField.builder()
                    .field("user")
                    .value(user.getId())
                    .operation("eq")
                    .values(new ArrayList<>())
                    .build());
        }
        return ResponseEntity.ok(notificationService.findBySearchCriteria(searchCriteria));
    }

    @GetMapping("/read-all")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public SuccessResponse readAll(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        notificationService.readAll(user.getId());
        return new SuccessResponse(true, "Notifications read");
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Notification not found")})
    public Notification getById(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Notification> optionalNotification = notificationService.findById(id);
        if (optionalNotification.isPresent()) {
            Notification savedNotification = optionalNotification.get();
            return savedNotification;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "Notification not found")})
    public Notification patch(@ApiParam("Notification") @Valid @RequestBody NotificationPatchDTO notification, @ApiParam("id") @PathVariable("id") Long id,
                              HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Notification> optionalNotification = notificationService.findById(id);

        if (optionalNotification.isPresent()) {
            Notification savedNotification = optionalNotification.get();
            return notificationService.update(id, notification);
        } else throw new CustomException("Notification not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("/push-token")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public SuccessResponse savePushToken(@RequestBody @Valid PushTokenPayload tokenPayload, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        String token = tokenPayload.getToken();
        PushNotificationToken pushNotificationToken;
        Optional<PushNotificationToken> optionalPushNotificationToken = pushNotificationTokenService.findByUser(user.getId());
        if (optionalPushNotificationToken.isPresent()) {
            pushNotificationToken = optionalPushNotificationToken.get();
            pushNotificationToken.setToken(token);
        } else {
            pushNotificationToken = PushNotificationToken.builder()
                    .user(user)
                    .token(token).build();
        }
        pushNotificationTokenService.save(pushNotificationToken);
        return new SuccessResponse(true, "Ok");
    }
}
