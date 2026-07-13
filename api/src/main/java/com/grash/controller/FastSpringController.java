package com.grash.controller;

import com.grash.dto.SuccessResponse;
import com.grash.dto.fastSpring.WebhookPayload;
import com.grash.dto.fastSpring.payloads.DeactivatedPayload;
import com.grash.dto.fastSpring.payloads.ResumePayload;
import com.grash.dto.fastSpring.payloads.SubscriptionCharge;
import com.grash.exception.CustomException;
import com.grash.model.OwnUser;
import com.grash.model.Subscription;
import com.grash.model.SubscriptionPlan;
import com.grash.model.enums.PlanFeatures;
import com.grash.service.SubscriptionPlanService;
import com.grash.service.SubscriptionService;
import com.grash.service.UserService;
import com.grash.service.WorkflowService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Date;
import java.util.Optional;

@RestController
@RequestMapping("/fast-spring")
@Api(tags = "fastSpring")
@RequiredArgsConstructor
@Transactional
public class FastSpringController {

    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final SubscriptionPlanService subscriptionPlanService;
    private final WorkflowService workflowService;

    @Value("${fast-spring.username:#{null}}")
    private String username;
    @Value("${fast-spring.password:#{null}}")
    private String password;

    @PostMapping("/new-subscription")
    public void onNewSubscription(@Valid @RequestBody WebhookPayload<com.grash.dto.fastSpring.payloads.SubscriptionPayload> subscription) {
        subscription.getEvents().forEach(event -> {
            long userId = event.getData().getTags().getUserId();
            Optional<OwnUser> optionalOwnUser = userService.findById(userId);
            if (optionalOwnUser.isPresent()) {
                OwnUser user = optionalOwnUser.get();
                Optional<Subscription> optionalSubscription =
                        subscriptionService.findById(user.getCompany().getSubscription().getId());
                if (optionalSubscription.isPresent()) {
                    Subscription savedSubscription = optionalSubscription.get();
                    com.grash.dto.fastSpring.payloads.SubscriptionPayload fastSpringSubscription = event.getData();
                    int newUsersCount = fastSpringSubscription.getQuantity();
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
                    savedSubscription.setUsersCount(newUsersCount);
                    setSubscriptionFromFastSpring(fastSpringSubscription.getProduct().getProduct(),
                            fastSpringSubscription.getId(),
                            fastSpringSubscription.getBegin(),
                            fastSpringSubscription.getNextChargeDate(),
                            savedSubscription, user.getCompany().getId());
                    subscriptionService.save(savedSubscription);
                } else throw new CustomException("Subscription not found", HttpStatus.NOT_FOUND);
            } else throw new CustomException("User Not Found", HttpStatus.NOT_FOUND);
        });
    }

    @PostMapping("/renew-subscription")
    public void onRenewSubscription(@Valid @RequestBody WebhookPayload<SubscriptionCharge> subscriptionChargeWebhookPayload) {
        subscriptionChargeWebhookPayload.getEvents().forEach(event -> {
            SubscriptionCharge subscriptionCharge = event.getData();
            long userId = subscriptionCharge.getSubscription().getTags().getUserId();
            Optional<OwnUser> optionalOwnUser = userService.findById(userId);
            if (optionalOwnUser.isPresent()) {
                OwnUser user = optionalOwnUser.get();
                Optional<Subscription> optionalSubscription =
                        subscriptionService.findById(user.getCompany().getSubscription().getId());
                if (optionalSubscription.isPresent()) {
                    Subscription savedSubscription = optionalSubscription.get();
                    setSubscriptionFromFastSpring(subscriptionCharge.getSubscription().getProduct(),
                            subscriptionCharge.getSubscription().getId(),
                            subscriptionCharge.getSubscription().getBegin(),
                            subscriptionCharge.getSubscription().getNextChargeDate(),
                            savedSubscription, user.getCompany().getId());
                    subscriptionService.save(savedSubscription);
                } else throw new CustomException("Subscription not found", HttpStatus.NOT_FOUND);
            } else throw new CustomException("User Not Found", HttpStatus.NOT_FOUND);
        });
    }

    @PostMapping("/deactivate")
    public void onDeactivatedSubscription(@Valid @RequestBody WebhookPayload<DeactivatedPayload> deactivatedPayloadWebhookPayload) {
        deactivatedPayloadWebhookPayload.getEvents().forEach(event -> {
                    Optional<Subscription> optionalSubscription =
                            subscriptionService.findByFastSpringId(event.getData().getSubscription());
                    if (optionalSubscription.isPresent()) {
                        Subscription savedSubscription = optionalSubscription.get();
                        subscriptionService.resetToFreePlan(savedSubscription);
                    } else throw new CustomException("Subscription Not found", HttpStatus.NOT_FOUND);
                }
        );
    }

    private void setSubscriptionFromFastSpring(String product, String id, long begin, long nextChargeDate,
                                               Subscription savedSubscription, Long companyId) {
        boolean monthly = product.contains("monthly");
        savedSubscription.setMonthly(monthly);
        savedSubscription.setActivated(true);
        SubscriptionPlan subscriptionPlan =
                subscriptionPlanService.findByCode(product.split("-")[0].toUpperCase()).get();
        if (subscriptionPlan.getFeatures().contains(PlanFeatures.WORKFLOW)) {
            workflowService.enableWorkflows(companyId);
        } else {
            workflowService.disableWorkflows(companyId);
        }
        savedSubscription.setFastSpringId(id);
        savedSubscription.setSubscriptionPlan(subscriptionPlan);
        savedSubscription.setStartsOn(new Date(begin));
        savedSubscription.setEndsOn(new Date(nextChargeDate));
        savedSubscription.setCancelled(false);
    }

    @GetMapping("/cancel")
    public SuccessResponse onCancel(HttpServletRequest req) {
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
            cancelRemoteSubscription(savedSubscription.getFastSpringId());
            savedSubscription.setCancelled(true);
            subscriptionService.save(savedSubscription);
            return new SuccessResponse(true, "Subscription cancelled");
        } else throw new CustomException("Subscription not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/resume")
    public SuccessResponse onResume(HttpServletRequest req) {
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
            resumeRemoteSubscription(savedSubscription.getFastSpringId());
            savedSubscription.setCancelled(false);
            subscriptionService.save(savedSubscription);
            return new SuccessResponse(true, "Subscription cancelled");
        } else throw new CustomException("Subscription not found", HttpStatus.NOT_FOUND);
    }

    private void cancelRemoteSubscription(String subscriptionId) {
        checkIfConfigured();
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(username, password);
        HttpEntity<String> request = new HttpEntity<>(headers);
        String url = "https://api.fastspring.com/subscriptions/" + subscriptionId;
        ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.DELETE,
                request, Object.class);
    }

    private void resumeRemoteSubscription(String subscriptionId) {
        checkIfConfigured();
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(username, password);
        ResumePayload resumePayload = new ResumePayload(subscriptionId, null);
        HttpEntity<ResumePayload> request = new HttpEntity<>(resumePayload, headers);
        String url = "https://api.fastspring.com/subscriptions/" + subscriptionId;
        ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.POST,
                request, Object.class);
    }

    private void checkIfConfigured() {
        if (username == null || password == null) {
            throw new CustomException("Fastspring not configured", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
