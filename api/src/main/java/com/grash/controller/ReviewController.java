package com.grash.controller;

import com.grash.dto.SuccessResponse;
import com.grash.model.User;
import com.grash.model.UserAppStats;
import com.grash.service.ReviewEligibilityService;
import com.grash.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

@RestController
@RequestMapping("/reviews")
@Tag(name = "Reviews", description = "Operations on in-app review prompts")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewEligibilityService reviewEligibilityService;
    private final UserService userService;

    @GetMapping("/eligibility")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Map<String, Boolean>> checkEligibility(HttpServletRequest req) {
        User user = userService.whoami(req);
        UserAppStats stats = reviewEligibilityService.getOrCreate(user);
        boolean eligible = reviewEligibilityService.isEligible(stats);
        return ResponseEntity.ok(Map.of("eligible", eligible));
    }

    @PostMapping("/mark-shown")
    @PreAuthorize("permitAll()")
    public ResponseEntity<SuccessResponse> markShown(HttpServletRequest req) {
        User user = userService.whoami(req);
        reviewEligibilityService.markReviewShown(reviewEligibilityService.getOrCreate(user));
        return ResponseEntity.ok(new SuccessResponse(true, "Review prompt marked as shown"));
    }

    @PostMapping("/clicked")
    @PreAuthorize("permitAll()")
    public ResponseEntity<SuccessResponse> markClicked(HttpServletRequest req) {
        User user = userService.whoami(req);
        reviewEligibilityService.markReviewClicked(reviewEligibilityService.getOrCreate(user));
        return ResponseEntity.ok(new SuccessResponse(true, "Review click recorded"));
    }

    @PostMapping("/rated")
    @PreAuthorize("permitAll()")
    public ResponseEntity<SuccessResponse> markRated(HttpServletRequest req) {
        User user = userService.whoami(req);
        reviewEligibilityService.markRated(reviewEligibilityService.getOrCreate(user));
        return ResponseEntity.ok(new SuccessResponse(true, "App rating recorded"));
    }

    @PostMapping("/session")
    @PreAuthorize("permitAll()")
    public ResponseEntity<SuccessResponse> incrementSession(HttpServletRequest req) {
        User user = userService.whoami(req);
        reviewEligibilityService.incrementSession(reviewEligibilityService.getOrCreate(user));
        return ResponseEntity.ok(new SuccessResponse(true, "Session count incremented"));
    }
}
