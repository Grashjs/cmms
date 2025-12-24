package com.grash.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grash.dto.keygen.*;
import com.grash.service.KeygenService;
import com.grash.service.StripeService;
import com.grash.service.EmailService2;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
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

    private final Map<String, Long> processedEvents = new ConcurrentHashMap<>();

    // TTL for processed events (24 hours in milliseconds)
    //TODO use redis
    private static final long EVENT_TTL = 24 * 60 * 60 * 1000;

    @Value("${mail.recipients:#{null}}")
    private String[] recipients;

    @Value("${stripe.webhook-secret-key}")
    private String stripeWebhookSecretKey;

    @PostMapping("/stripe-webhook")
    public ResponseEntity<String> handleWebhook(HttpServletRequest request) throws IOException {
        String payload = request.getReader().lines().collect(Collectors.joining("\n"));
        Event event;
        String sigHeader = request.getHeader("Stripe-Signature");

        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecretKey);
        } catch (SignatureVerificationException e) {
            log.error("Invalid signature: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        if (isDuplicate(event.getId())) {
            log.info("Duplicate event detected: {}, skipping processing", event.getId());
            return ResponseEntity.ok("Already processed");
        }

        // Clean up old events periodically
        cleanupOldEvents();

        // Handle the event
        switch (event.getType()) {
            case "checkout.session.completed":
                Session session = (Session) event.getData()
                        .getObject();

                if (session != null) {
                    handleCheckoutSessionCompleted(session, event.getId());
                }
                break;
            default:
                log.info("Unhandled event type: {}", event.getType());
        }

        // Mark event as processed
        markAsProcessed(event.getId());

        return ResponseEntity.ok("Success");
    }

    /**
     * Check if an event has already been processed
     */
    private boolean isDuplicate(String eventId) {
        return processedEvents.containsKey(eventId);
    }

    /**
     * Mark an event as processed with timestamp
     */
    private void markAsProcessed(String eventId) {
        processedEvents.put(eventId, System.currentTimeMillis());
    }

    /**
     * Clean up events older than TTL to prevent memory bloat
     */
    private void cleanupOldEvents() {
        long currentTime = System.currentTimeMillis();
        processedEvents.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > EVENT_TTL
        );
    }

    private void handleCheckoutSessionCompleted(Session session, String eventId) {
        String email = Optional.ofNullable(session.getCustomerEmail())
                .orElse(session.getCustomerDetails().getEmail());
        String planId = session.getMetadata().get("planId");

        log.info("Processing checkout for email: {}, plan: {}, eventId: {}",
                email, planId, eventId);

        try {
            log.info("Creating license for keygen user {} with plan {}", email, planId);
            KeygenLicenseResponse keygenLicenseResponse = keygenService.createLicense(planId, email);

            Map<String, Object> model = new HashMap<>();
            model.put("name", session.getCustomerDetails().getName());
            model.put("plan", planId);
            model.put("licenseKey", keygenLicenseResponse.getData().getAttributes().getKey());
            model.put("expiringAt", keygenLicenseResponse.getData().getAttributes().getExpiry());

            emailService.sendMessageUsingThymeleafTemplate(
                    new String[]{email},
                    "Atlas CMMS license key",
                    model,
                    "checkout-complete.html",
                    Locale.getDefault()
            );

            log.info("Successfully processed checkout for email: {}", email);
        } catch (Exception e) {
            log.error("Failed to create license for keygen user {}", email, e);

            if (recipients != null && recipients.length > 0) {
                emailService.sendSimpleMessage(
                        recipients,
                        "Failed to create license for keygen user " + email,
                        "Failed to create license for keygen user " + email +
                                "\nEvent ID: " + eventId +
                                "\nError: " + e.getMessage()
                );
            }

            // Remove from processed events so it can be retried
            processedEvents.remove(eventId);

            throw new RuntimeException("Failed to process checkout", e);
        }
    }
}