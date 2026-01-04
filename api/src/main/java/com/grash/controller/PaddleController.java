package com.grash.controller;

import com.grash.dto.SuccessResponse;
import com.grash.dto.checkout.CheckoutRequest;
import com.grash.dto.checkout.CheckoutResponse;
import com.grash.exception.CustomException;
import com.grash.model.OwnUser;
import com.grash.model.Subscription;
import com.grash.model.enums.SubscriptionScheduledChangeType;
import com.grash.service.PaddleService;
import com.grash.service.SubscriptionService;
import com.grash.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Optional;

@RestController
@RequestMapping("/paddle")
@RequiredArgsConstructor
public class PaddleController {

    private final PaddleService paddleService;
    private final UserService userService;
    private final SubscriptionService subscriptionService;
    @Value("${cloud-version}")
    private boolean cloudVersion;

    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @GetMapping("/cancel")
    public SuccessResponse cancel(HttpServletRequest req) {
        checkIfCloudVersion();
        OwnUser user = userService.whoami(req);
        Optional<Subscription> optionalSubscription =
                subscriptionService.findById(user.getCompany().getSubscription().getId());
        if (optionalSubscription.isPresent()) {
            Subscription savedSubscription = optionalSubscription.get();
            if (!savedSubscription.isActivated()) {
                throw new CustomException("Subscription is not activated", HttpStatus.NOT_ACCEPTABLE);
            }
            if (savedSubscription.getScheduledChangeType() == SubscriptionScheduledChangeType.RESET_TO_FREE) {
                throw new CustomException("Subscription already cancelled", HttpStatus.NOT_ACCEPTABLE);
            }
            paddleService.pauseSubscription(savedSubscription.getPaddleSubscriptionId());
            return new SuccessResponse(true, "Subscription cancelled");
        } else throw new CustomException("Subscription not found", HttpStatus.NOT_FOUND);
    }

    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @GetMapping("/resume")
    public SuccessResponse resume(HttpServletRequest req) {
        checkIfCloudVersion();
        OwnUser user = userService.whoami(req);
        Optional<Subscription> optionalSubscription =
                subscriptionService.findById(user.getCompany().getSubscription().getId());
        if (optionalSubscription.isPresent()) {
            Subscription savedSubscription = optionalSubscription.get();
            if (!savedSubscription.isActivated()) {
                throw new CustomException("Subscription is not activated", HttpStatus.NOT_ACCEPTABLE);
            }
            paddleService.resumeSubscription(savedSubscription.getPaddleSubscriptionId());
            return new SuccessResponse(true, "Subscription resumed");
        } else throw new CustomException("Subscription not found", HttpStatus.NOT_FOUND);
    }

    private void checkIfCloudVersion() {
        if (!cloudVersion) throw new CustomException("Paddle Cloud is not enabled", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/create-checkout-session")
    public CheckoutResponse createCheckoutSession(@Valid @RequestBody CheckoutRequest request) {
        return paddleService.createCheckoutSession(request);
    }
}