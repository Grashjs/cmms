package com.grash.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grash.dto.keygen.*;
import com.grash.dto.stripe.StripeCustomer;
import com.grash.dto.stripe.StripeWebhookRequest;
import com.grash.service.KeygenService;
import com.grash.service.StripeService;
import com.stripe.model.Customer;
import com.stripe.model.Subscription;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
class WebhookController {

    private final KeygenService keygenService;
    private final StripeService stripeService;
    private final ObjectMapper objectMapper;

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

    @PostMapping(value = "/stripe-webhook", consumes = "application/json")
    public ResponseEntity<Void> handleStripeWebhook(@RequestBody StripeWebhookRequest request) {
        try {
            if ("customer.created".equals(request.getType())) {
                handleCustomerCreated(request.getData().getCustomer());
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private void handleCustomerCreated(StripeCustomer customer) throws Exception {
        String keygenUserId = customer.getMetadata().get("keygenUserId");
        if (keygenUserId == null) {
            throw new RuntimeException("Customer does not have a Keygen user ID");
        }

        Subscription subscription = stripeService.createSubscription(customer.getId(), keygenUserId);

        KeygenLicenseResponse response = keygenService.createLicense(keygenUserId, subscription.getId());

        if (response.getErrors() != null) {
            throw new RuntimeException("Failed to create license");
        }
    }
}