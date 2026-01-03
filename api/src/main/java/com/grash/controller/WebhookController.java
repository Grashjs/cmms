package com.grash.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grash.dto.keygen.KeygenLicenseResponse;
import com.grash.dto.keygen.KeygenLicenseResponseData;
import com.grash.dto.paddle.BillingDetails;
import com.grash.dto.paddle.subscription.PaddleSubscriptionData;
import com.grash.dto.paddle.subscription.PaddleSubscriptionWebhookEvent;
import com.grash.exception.CustomException;
import com.grash.model.OwnUser;
import com.grash.model.Subscription;
import com.grash.model.enums.SubscriptionScheduledChangeType;
import com.grash.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Slf4j
class WebhookController {

    private final KeygenService keygenService;
    private final EmailService2 emailService;
    private final ObjectMapper objectMapper;
    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final PaddleService paddleService;
    private final Scheduler scheduler;

    @Value("${cloud-version}")
    private boolean cloudVersion;

    private final Map<String, Long> processedEvents = new ConcurrentHashMap<>();
    private static final long EVENT_TTL = 24 * 60 * 60 * 1000;

    @Value("${mail.recipients:#{null}}")
    private String[] recipients;

    @Value("${paddle.webhook-secret-key}")
    private String paddleWebhookSecretKey;

    @PostMapping("/paddle-webhook")
    public ResponseEntity<String> handleWebhook(HttpServletRequest request) throws IOException {
        String payload = request.getReader().lines().collect(Collectors.joining("\n"));
        String signature = request.getHeader("Paddle-Signature");

        if (!verifyWebhookSignature(payload, signature)) {
            log.error("Invalid Paddle webhook signature");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        try {
            PaddleSubscriptionWebhookEvent webhookEvent = objectMapper.readValue(payload,
                    PaddleSubscriptionWebhookEvent.class);
            String eventType = webhookEvent.getEventType();
            String eventId = webhookEvent.getEventId();

            if (isDuplicate(eventId)) {
                log.info("Duplicate event detected: {}, skipping processing", eventId);
                return ResponseEntity.ok("Already processed");
            }

            cleanupOldEvents();

            switch (eventType) {
                case "subscription.created":
                    handleSubscriptionCreated(webhookEvent, eventId);
                    break;
                case "subscription.updated":
                    handleSubscriptionUpdated(webhookEvent, eventId);
                    break;
                case "subscription.paused":
                    handleSubscriptionPaused(webhookEvent, eventId, false);
                    break;
                case "subscription.resumed":
                    handleSubscriptionResumed(webhookEvent, eventId);
                    break;
                case "subscription.canceled":
                    handleSubscriptionPaused(webhookEvent, eventId, true);
                    break;
                default:
                    log.info("Unhandled event type: {}", eventType);
            }

            markAsProcessed(eventId);
            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            log.error("Error processing Paddle webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing webhook");
        }
    }

    private void handleSubscriptionUpdated(PaddleSubscriptionWebhookEvent webhookEvent, String eventId) {
        PaddleSubscriptionData data = webhookEvent.getData();
        if (data.getCustomData() != null && data.getCustomData().containsKey("userId")) {
            handleCloudSubscriptionUpdated(webhookEvent, eventId, false);
        } else {
            handleSelfHostedSubscriptionUpdated(webhookEvent, eventId);
        }
    }

    /**
     * Handle regular subscription updates (plan changes, quantity changes, etc.)
     * Does NOT handle pause/cancel/resume - those have dedicated events
     */
    private void handleCloudSubscriptionUpdated(PaddleSubscriptionWebhookEvent webhookEvent, String eventId,
                                                boolean isNewSubscription) {
        checkIfCloudVersion();
        PaddleSubscriptionData data = webhookEvent.getData();
        long userId = Long.parseLong(data.getCustomData().get("userId"));

        Optional<OwnUser> optionalOwnUser = userService.findById(userId);
        if (!optionalOwnUser.isPresent()) {
            throw new CustomException("User Not Found", HttpStatus.NOT_FOUND);
        }

        OwnUser user = optionalOwnUser.get();
        Subscription savedSubscription = user.getCompany().getSubscription();

        String planCode = data.getCustomData().get("planId");
        int newUsersCount = data.getItems().get(0).getQuantity();


        if (data.getScheduledChange() != null) {
            // Parse the scheduled change date from Paddle data
            Date scheduledChangeDate = parseDate(data.getScheduledChange().getEffectiveAt());
            if (scheduledChangeDate != null) {
                switch (data.getScheduledChange().getAction()) {
                    case "pause":
                    case "cancel":
                        savedSubscription.setScheduledChangeDate(scheduledChangeDate);
                        savedSubscription.setScheduledChangeType(SubscriptionScheduledChangeType.RESET_TO_FREE);
                        log.info("Scheduled subscription reset to free plan for paddle subscription ID: {} at" +
                                        " {}," +
                                        " " +
                                        "eventId: {}",
                                data.getId(), scheduledChangeDate, eventId);
                        break;
                    default:
                        break;
                }
            }
        }
        if (isNewSubscription) {
            savedSubscription.setScheduledChangeDate(null);
            savedSubscription.setScheduledChangeType(null);
        }
        // Update subscription details (plan, quantity, billing period)
        paddleService.updateSubscription(
                savedSubscription,
                planCode,
                data.getId(),
                parseDate(data.getCurrentBillingPeriod().getStartsAt()),
                parseDate(data.getCurrentBillingPeriod().getEndsAt()),
                user.getCompany().getId(),
                newUsersCount
        );

        subscriptionService.save(savedSubscription);
        log.info("Successfully updated cloud subscription for user ID: {}, eventId: {}", userId, eventId);
    }

    /**
     * Handle subscription pause event
     * This is fired when the subscription actually becomes paused
     */
    private void handleSubscriptionPaused(PaddleSubscriptionWebhookEvent webhookEvent, String eventId,
                                          boolean cancelled) {
        PaddleSubscriptionData data = webhookEvent.getData();

        // Only handle cloud subscriptions
        if (data.getCustomData() == null || !data.getCustomData().containsKey("userId")) {
            log.info("Ignoring pause event for self-hosted subscription: {}", data.getId());
            return;
        }

        checkIfCloudVersion();
        long userId = Long.parseLong(data.getCustomData().get("userId"));

        Optional<OwnUser> optionalOwnUser = userService.findById(userId);
        if (!optionalOwnUser.isPresent()) {
            throw new CustomException("User Not Found", HttpStatus.NOT_FOUND);
        }

        OwnUser user = optionalOwnUser.get();
        Subscription subscription = user.getCompany().getSubscription();

        if (subscription == null) {
            throw new CustomException("Subscription not found", HttpStatus.NOT_FOUND);
        }
        if (cancelled) subscription.setPaddleSubscriptionId(null);
        subscriptionService.resetToFreePlan(subscription);

        log.info("Subscription paused for user ID: {}, eventId: {}",
                userId, eventId);
    }

    /**
     * Handle subscription resume event
     * This is fired when a paused subscription is resumed
     */
    private void handleSubscriptionResumed(PaddleSubscriptionWebhookEvent webhookEvent, String eventId) {
        handleCloudSubscriptionUpdated(webhookEvent, eventId, true);
    }

    private boolean verifyWebhookSignature(String payload, String signature) {
        if (signature == null || signature.isEmpty()) {
            log.error("Missing Paddle-Signature header");
            return false;
        }

        try {
            Map<String, String> signatureParts = new HashMap<>();
            for (String part : signature.split(";")) {
                String[] keyValue = part.split("=", 2);
                if (keyValue.length == 2) {
                    signatureParts.put(keyValue[0], keyValue[1]);
                }
            }

            String timestamp = signatureParts.get("ts");
            String receivedSignature = signatureParts.get("h1");

            if (timestamp == null || receivedSignature == null) {
                log.error("Invalid signature format");
                return false;
            }

            String signedPayload = timestamp + ":" + payload;

            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    paddleWebhookSecretKey.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            String calculatedSignature = hexString.toString();

            return MessageDigest.isEqual(
                    calculatedSignature.getBytes(StandardCharsets.UTF_8),
                    receivedSignature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Error verifying webhook signature", e);
            return false;
        }
    }

    private boolean isDuplicate(String eventId) {
        return processedEvents.containsKey(eventId);
    }

    private void markAsProcessed(String eventId) {
        processedEvents.put(eventId, System.currentTimeMillis());
    }

    private void cleanupOldEvents() {
        long currentTime = System.currentTimeMillis();
        processedEvents.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > EVENT_TTL
        );
    }

    private void handleSubscriptionCreated(PaddleSubscriptionWebhookEvent webhookEvent, String eventId) {
        PaddleSubscriptionData data = webhookEvent.getData();
        if (data.getCustomData() != null && data.getCustomData().containsKey("userId")) {
            handleCloudSubscriptionUpdated(webhookEvent, eventId, true);
        } else {
            handleSelfHostedSubscriptionCreated(webhookEvent, eventId);
        }
    }

    private void handleSelfHostedSubscriptionCreated(PaddleSubscriptionWebhookEvent webhookEvent, String eventId) {
        try {
            PaddleSubscriptionData data = webhookEvent.getData();

            String email = data.getCustomData() != null ? data.getCustomData().get("email") : null;
            String planId = data.getCustomData() != null ? data.getCustomData().get("planId") : null;
            Integer quantity = data.getItems().get(0).getQuantity();

            if (email == null) {
                log.error("Email not found in custom_data for transaction");
                throw new RuntimeException("Email missing from transaction");
            }
            if (planId == null) {
                log.error("Plan ID not found in custom_data for transaction");
                throw new RuntimeException("Plan ID missing from transaction");
            }

            String customerName = Optional.ofNullable(data.getBillingDetails())
                    .map(BillingDetails::getCustomerName)
                    .orElse(email);

            log.info("Processing Paddle transaction for email: {}, plan: {}, eventId: {}",
                    email, planId, eventId);

            log.info("Creating license for keygen user {} with plan {}", email, planId);
            KeygenLicenseResponse keygenLicenseResponse = keygenService.createLicense(planId, email, quantity,
                    data.getId());

            Map<String, Object> model = new HashMap<>();
            model.put("name", customerName);
            model.put("plan", planId);
            model.put("usersCount", quantity);
            model.put("licenseKey", keygenLicenseResponse.getData().getAttributes().getKey());
            model.put("expiringAt", keygenLicenseResponse.getData().getAttributes().getExpiry());

            emailService.sendMessageUsingThymeleafTemplate(
                    new String[]{email},
                    "Atlas CMMS license key",
                    model,
                    "checkout-complete.html",
                    Locale.getDefault()
            );

            log.info("Successfully processed Paddle transaction for email: {}", email);
        } catch (Exception e) {
            log.error("Failed to process Paddle transaction", e);

            if (recipients != null && recipients.length > 0) {
                emailService.sendSimpleMessage(
                        recipients,
                        "Failed to process Paddle transaction",
                        "Failed to process Paddle transaction" +
                                "\nEvent ID: " + eventId +
                                "\nError: " + e.getMessage()
                );
            }

            processedEvents.remove(eventId);
            throw new RuntimeException("Failed to process transaction", e);
        }
    }

    private void handleSelfHostedSubscriptionUpdated(PaddleSubscriptionWebhookEvent webhookEvent, String eventId) {
        try {
            PaddleSubscriptionData data = webhookEvent.getData();
            String paddleSubscriptionId = data.getId();
            String email = data.getCustomData() != null ? data.getCustomData().get("email") : null;
            String planId = data.getCustomData() != null ? data.getCustomData().get("planId") : null;
            Integer quantity = data.getItems().get(0).getQuantity();

            String customerName = Optional.ofNullable(data.getBillingDetails())
                    .map(BillingDetails::getCustomerName)
                    .orElse(email);

            if (!"active".equalsIgnoreCase(data.getStatus())) {
                log.info("Subscription {} status is '{}', not an active renewal. Skipping.", paddleSubscriptionId,
                        data.getStatus());
                return;
            }

            log.info("Processing subscription renewal for Paddle subscription ID: {}, eventId: {}",
                    paddleSubscriptionId, eventId);

            KeygenLicenseResponseData license = keygenService.getLicenseByPaddleSubscriptionId(paddleSubscriptionId);

            if (license == null) {
                log.error("No license found for Paddle subscription ID: {}", paddleSubscriptionId);
                throw new RuntimeException("License not found for paddle subscription " + paddleSubscriptionId);
            }

            String licenseId = license.getId();
            log.info("Found license {} for renewal. Extending expiry.", licenseId);

            String newExpiry = data.getNextBilledAt();
            if (newExpiry == null) {
                log.error("next_billed_at is null for paddle subscription {}", paddleSubscriptionId);
                throw new RuntimeException("next_billed_at is null for paddle subscription " + paddleSubscriptionId);
            }

            keygenService.extendLicense(licenseId, newExpiry);

            Map<String, Object> model = new HashMap<>();
            model.put("name", customerName);
            model.put("plan", planId);
            model.put("usersCount", quantity);
            model.put("licenseKey", license.getAttributes().getKey());
            model.put("expiringAt", license.getAttributes().getExpiry());

            emailService.sendMessageUsingThymeleafTemplate(
                    new String[]{email},
                    "Atlas CMMS license key renewal",
                    model,
                    "checkout-complete.html",
                    Locale.getDefault()
            );
            log.info("Successfully extended license {} for Paddle subscription ID: {}", licenseId,
                    paddleSubscriptionId);

        } catch (Exception e) {
            log.error("Failed to process subscription renewal for eventId: {}", eventId, e);

            if (recipients != null && recipients.length > 0) {
                emailService.sendSimpleMessage(
                        recipients,
                        "Failed to process subscription renewal",
                        "Failed to process subscription renewal" +
                                "\nEvent ID: " + eventId +
                                "\nError: " + e.getMessage()
                );
            }
            processedEvents.remove(eventId);
            throw new RuntimeException("Failed to process subscription renewal", e);
        }
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