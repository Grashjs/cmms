package com.grash.controller;

import com.grash.dto.SuccessResponse;
import com.grash.model.User;
import com.grash.service.SubscriptionService;
import com.grash.service.UserService;


import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Collection;

@RestController
@RequestMapping("/subscriptions")
@Tag(name = "Subscriptions", description = "Operations on subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserService userService;

    @PostMapping("/upgrade")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public SuccessResponse upgrade(@Parameter(description = "List of user IDs to upgrade") @RequestBody Collection<Long> usersIds,
                                   HttpServletRequest req) {
        User user = userService.whoami(req);
        subscriptionService.upgrade(usersIds, user);
        return new SuccessResponse(true, "Users enabled successfully");
    }
    

    @GetMapping("/downgrade")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public SuccessResponse downgrade(@Parameter(description = "Collection of user IDs to downgrade") @RequestParam Collection<Long> usersIds,
                                     HttpServletRequest req) {
        User user = userService.whoami(req);
        subscriptionService.downgrade(usersIds, user);
        return new SuccessResponse(true, "Users disabled successfully");
    }
}
