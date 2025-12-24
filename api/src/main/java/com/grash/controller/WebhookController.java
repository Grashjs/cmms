package com.grash.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grash.dto.keygen.*;
import com.grash.service.KeygenService;
import com.grash.service.StripeService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Slf4j
class WebhookController {

    private final KeygenService keygenService;
    private final StripeService stripeService;
    private final ObjectMapper objectMapper;
    @Value("${stripe.webhook-secret-key}")
    private String stripeWebhookSecretKey;

    @PostMapping(value = "/keygen-webhook", consumes = {"application/vnd.api+json", "application/json"})
    public ResponseEntity<Void> handleKeygenWebhook(@RequestBody KeygenWebhookRequest request) {
        try {
            String eventId = request.getData().getId();
            KeygenEventResponse eventResponse = keygenService.fetchWebhookEvent(eventId);

            if (eventResponse.getData() == null || eventResponse.getErrors() != null) {
                return ResponseEntity.ok().build();
            }

            KeygenEvent event = eventResponse.getData();
            String eventType = event.getAttributes().getEvent();

            if ("user.created".equals(eventType)) {
                handleUserCreated(event.getAttributes());
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private void handleUserCreated(KeygenEventAttributes attributes) throws Exception {
        String payloadJson = attributes.getPayload();
        KeygenUserPayload payload = objectMapper.readValue(payloadJson, KeygenUserPayload.class);
        KeygenUser user = payload.getData();

        String stripeToken = (String) user.getAttributes().getMetadata().get("stripeToken");
        if (stripeToken == null) {
            throw new RuntimeException("User does not have a Stripe token");
        }

        Customer customer = stripeService.createCustomer(
                user.getAttributes().getEmail(),
                "Customer for Keygen user " + user.getAttributes().getEmail(),
                stripeToken,
                user.getId()
        );

        Map<String, String> metadata = new HashMap<>();
        metadata.put("stripeCustomerId", customer.getId());
        keygenService.updateUserMetadata(user.getId(), metadata);
    }

    @PostMapping("/stripe-webhook")
    public ResponseEntity<String> handleWebhook(
            HttpServletRequest request) throws IOException {
        String payload = request.getReader().lines().collect(Collectors.joining("\n"));

        Event event;
        // 2. Get the Stripe-Signature header
        String sigHeader = request.getHeader("Stripe-Signature");
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecretKey);
        } catch (SignatureVerificationException e) {
            log.error("Invalid signature: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        // Handle the event
        switch (event.getType()) {
            case "checkout.session.completed":
                Session session = (Session) event.getData()
                        .getObject();

                if (session != null) {
                    handleCheckoutSessionCompleted(session);
                }
                break;

            case "payment_intent.succeeded":
                log.info("Payment succeeded: {}", event.getId());
                break;

            default:
                log.info("Unhandled event type: {}", event.getType());
        }

        return ResponseEntity.ok("Success");
    }

    private void handleCheckoutSessionCompleted(Session session) {
        String email = Optional.ofNullable(session.getCustomerEmail()).orElse(session.getCustomerDetails().getEmail());
        String planId = session.getMetadata().get("planId");
        String keygenUserId = "keygen_user_" + email;

        log.info("Checkout completed for email: {}, plan: {}", email, planId);
        try {
            log.info("Creating license for keygen user {} with plan {}", keygenUserId, planId);
            keygenService.createLicense(planId, keygenUserId);
            
            // TODO: Implement email sending logic, e.g., via an EmailService
            log.info("Skipping confirmation email for now.");

        } catch (Exception e) {
            log.error("Failed to create license for keygen user {}", keygenUserId, e);
            // Optional: Add to a retry queue or notify administrators
        }
    }
}