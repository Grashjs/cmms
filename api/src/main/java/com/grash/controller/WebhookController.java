package com.grash.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grash.dto.keygen.*;
import com.grash.dto.paddle.BillingDetails;
import com.grash.dto.paddle.PaddleTransactionData;
import com.grash.dto.paddle.PaddleWebhookEvent;
import com.grash.service.KeygenService;
import com.grash.service.EmailService2;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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

    private final Map<String, Long> processedEvents = new ConcurrentHashMap<>();

    // TTL for processed events (24 hours in milliseconds)
    //TODO use redis
    private static final long EVENT_TTL = 24 * 60 * 60 * 1000;

    @Value("${mail.recipients:#{null}}")
    private String[] recipients;

    @Value("${paddle.webhook-secret-key}")
    private String paddleWebhookSecretKey;

    @PostMapping("/paddle-webhook")
    public ResponseEntity<String> handleWebhook(HttpServletRequest request) throws IOException {
        String payload = request.getReader().lines().collect(Collectors.joining("\n"));
        String signature = request.getHeader("Paddle-Signature");

        // Verify webhook signature
        if (!verifyWebhookSignature(payload, signature)) {
            log.error("Invalid Paddle webhook signature");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        try {
            PaddleWebhookEvent webhookEvent = objectMapper.readValue(payload, PaddleWebhookEvent.class);
            String eventType = webhookEvent.getEventType();
            String eventId = webhookEvent.getEventId();

            if (isDuplicate(eventId)) {
                log.info("Duplicate event detected: {}, skipping processing", eventId);
                return ResponseEntity.ok("Already processed");
            }

            // Clean up old events periodically
            cleanupOldEvents();

            if (eventType.equals("transaction.paid")) {
                handleTransactionCompleted(webhookEvent, eventId);
            } else {
                log.info("Unhandled event type: {}", eventType);
            }

            // Mark event as processed
            markAsProcessed(eventId);

            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            log.error("Error processing Paddle webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing webhook");
        }
    }

    /**
     * Verify Paddle webhook signature using HMAC SHA256
     */
    private boolean verifyWebhookSignature(String payload, String signature) {
        if (signature == null || signature.isEmpty()) {
            log.error("Missing Paddle-Signature header");
            return false;
        }

        try {
            // Parse signature header
            // Format: ts=timestamp;h1=signature
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

            // Create signed payload: timestamp:payload
            String signedPayload = timestamp + ":" + payload;

            // Calculate HMAC SHA256
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    paddleWebhookSecretKey.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));

            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            String calculatedSignature = hexString.toString();

            // Compare signatures (constant-time comparison)
            return MessageDigest.isEqual(
                    calculatedSignature.getBytes(StandardCharsets.UTF_8),
                    receivedSignature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Error verifying webhook signature", e);
            return false;
        }
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

    private void handleTransactionCompleted(PaddleWebhookEvent webhookEvent, String eventId) {
        try {
            PaddleTransactionData data = webhookEvent.getData();

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
            // Get customer name from billing details or fallback to email
            String customerName = Optional.ofNullable(data.getBillingDetails())
                    .map(BillingDetails::getCustomerName)
                    .orElse(email);

            log.info("Processing Paddle transaction for email: {}, plan: {}, eventId: {}",
                    email, planId, eventId);

            log.info("Creating license for keygen user {} with plan {}", email, planId);
            KeygenLicenseResponse keygenLicenseResponse = keygenService.createLicense(planId, email, quantity);

            Map<String, Object> model = new HashMap<>();
            model.put("name", customerName);
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

            // Remove from processed events so it can be retried
            processedEvents.remove(eventId);

            throw new RuntimeException("Failed to process transaction", e);
        }
    }
}