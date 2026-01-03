package com.grash.controller;

import com.grash.dto.SuccessResponse;
import com.grash.dto.paddle.subscription.PaddleSubscriptionData;
import com.grash.dto.paddle.subscription.PaddleSubscriptionWebhookEvent;
import com.grash.exception.CustomException;
import com.grash.model.OwnUser;
import com.grash.model.Subscription;
import com.grash.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

@RestController
@RequestMapping("/paddle/cloud")
@RequiredArgsConstructor
@Transactional
public class PaddleCloudController {

    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final PaddleService paddleService;
    @Value("${cloud-version}")
    private boolean cloudVersion;

    @PostMapping("/new-subscription")
    public void onNewSubscription(@Valid @RequestBody PaddleSubscriptionWebhookEvent webhookEvent) {
        checkIfCloudVersion();
        PaddleSubscriptionData data = webhookEvent.getData();
        long userId = Long.parseLong(data.getCustomData().get("userId"));
        Optional<OwnUser> optionalOwnUser = userService.findById(userId);
        if (optionalOwnUser.isPresent()) {
            OwnUser user = optionalOwnUser.get();
            Optional<Subscription> optionalSubscription =
                    subscriptionService.findById(user.getCompany().getSubscription().getId());
            if (optionalSubscription.isPresent()) {
                Subscription savedSubscription = optionalSubscription.get();
                int newUsersCount = data.getItems().get(0).getQuantity();
                int subscriptionUsersCount =
                        (int) userService.findByCompany(user.getCompany().getId()).stream().filter(OwnUser::isEnabledInSubscriptionAndPaid).count();
                if (newUsersCount < subscriptionUsersCount) {
                    savedSubscription.setDowngradeNeeded(true);
                } else {
                    int usersNotInSubscriptionCount =
                            (int) userService.findByCompany(user.getCompany().getId()).stream().filter(user1 -> !user1.isEnabledInSubscription()).count();
                    if (usersNotInSubscriptionCount > 0) {
                        savedSubscription.setUpgradeNeeded(true);
                    }
                }
                String planCode = data.getCustomData().get("planId");

                paddleService.updateSubscription(savedSubscription, planCode, data.getId(),
                        new Date(), parseDate(data.getNextBilledAt()), user.getCompany().getId(), newUsersCount);

                subscriptionService.save(savedSubscription);
            } else throw new CustomException("Subscription not found", HttpStatus.NOT_FOUND);
        } else throw new CustomException("User Not Found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("/renew-subscription")
    public void onRenewSubscription(@Valid @RequestBody PaddleSubscriptionWebhookEvent webhookEvent) {
        checkIfCloudVersion();
        PaddleSubscriptionData data = webhookEvent.getData();
        long userId = Long.parseLong(data.getCustomData().get("userId"));
        Optional<OwnUser> optionalOwnUser = userService.findById(userId);
        if (optionalOwnUser.isPresent()) {
            OwnUser user = optionalOwnUser.get();
            Optional<Subscription> optionalSubscription =
                    subscriptionService.findById(user.getCompany().getSubscription().getId());
            if (optionalSubscription.isPresent()) {
                Subscription savedSubscription = optionalSubscription.get();
                String planCode = data.getCustomData().get("planId");
                int newUsersCount = data.getItems().get(0).getQuantity();

                paddleService.updateSubscription(savedSubscription, planCode, data.getId(),
                        new Date(), parseDate(data.getNextBilledAt()), user.getCompany().getId(), newUsersCount);

                subscriptionService.save(savedSubscription);
            } else throw new CustomException("Subscription not found", HttpStatus.NOT_FOUND);
        } else throw new CustomException("User Not Found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("/deactivate")
    public void onDeactivatedSubscription(@Valid @RequestBody PaddleSubscriptionWebhookEvent webhookEvent) {
        checkIfCloudVersion();
        Optional<Subscription> optionalSubscription =
                subscriptionService.findByPaddleSubscriptionId(webhookEvent.getData().getId());
        if (optionalSubscription.isPresent()) {
            Subscription savedSubscription = optionalSubscription.get();
            subscriptionService.resetToFreePlan(savedSubscription);
        } else throw new CustomException("Subscription Not found", HttpStatus.NOT_FOUND);
    }

    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @GetMapping("/cancel")
    public SuccessResponse onCancel(HttpServletRequest req) {
        checkIfCloudVersion();
        OwnUser user = userService.whoami(req);
        Optional<Subscription> optionalSubscription =
                subscriptionService.findById(user.getCompany().getSubscription().getId());
        if (optionalSubscription.isPresent()) {
            Subscription savedSubscription = optionalSubscription.get();
            if (!savedSubscription.isActivated()) {
                throw new CustomException("Subscription is not activated", HttpStatus.NOT_ACCEPTABLE);
            }
            if (savedSubscription.isCancelled()) {
                throw new CustomException("Subscription already cancelled", HttpStatus.NOT_ACCEPTABLE);
            }
            paddleService.cancelSubscription(savedSubscription.getPaddleSubscriptionId());
            savedSubscription.setCancelled(true);
            subscriptionService.save(savedSubscription);
            return new SuccessResponse(true, "Subscription cancelled");
        } else throw new CustomException("Subscription not found", HttpStatus.NOT_FOUND);
    }

    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @GetMapping("/resume")
    public SuccessResponse onResume(HttpServletRequest req) {
        checkIfCloudVersion();
        OwnUser user = userService.whoami(req);
        Optional<Subscription> optionalSubscription =
                subscriptionService.findById(user.getCompany().getSubscription().getId());
        if (optionalSubscription.isPresent()) {
            Subscription savedSubscription = optionalSubscription.get();
            if (!savedSubscription.isActivated()) {
                throw new CustomException("Subscription is not activated", HttpStatus.NOT_ACCEPTABLE);
            }
            if (!savedSubscription.isCancelled()) {
                throw new CustomException("Subscription is active", HttpStatus.NOT_ACCEPTABLE);
            }
            paddleService.resumeSubscription(savedSubscription.getPaddleSubscriptionId());
            savedSubscription.setCancelled(false);
            subscriptionService.save(savedSubscription);
            return new SuccessResponse(true, "Subscription resumed");
        } else throw new CustomException("Subscription not found", HttpStatus.NOT_FOUND);
    }

    private Date parseDate(String dateStr) {
        if (dateStr == null) return null;
        try {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'").parse(dateStr);
        } catch (ParseException e) {
            try {
                return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(dateStr);
            } catch (ParseException e1) {
                return null;
            }
        }
    }

    private void checkIfCloudVersion() {
        if (!cloudVersion) throw new CustomException("Paddle Cloud is not enabled", HttpStatus.FORBIDDEN);
    }
}
