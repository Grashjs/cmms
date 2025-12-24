package com.grash.service;

import com.grash.dto.stripe.CheckoutRequest;
import com.grash.dto.stripe.CheckoutResponse;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class StripeService {

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.plan-id}")
    private String stripePlanId;

    @Value("${frontend.url}")
    private String frontendUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    public Customer createCustomer(String email, String description, String stripeToken, String keygenUserId) throws Exception {
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setDescription(description)
                .setEmail(email)
                .setSource(stripeToken)
                .putMetadata("keygenUserId", keygenUserId)
                .build();

        return Customer.create(params);
    }

    public Subscription createSubscription(String customerId, String idempotencyKey) throws Exception {

        SubscriptionCreateParams params = SubscriptionCreateParams.builder()
                .setCustomer(customerId)
                .addItem(SubscriptionCreateParams.Item.builder()
                        .setPrice(stripePlanId)
                        .build())
                .build();

        return Subscription.create(params,
                com.stripe.net.RequestOptions.builder()
                        .setIdempotencyKey(idempotencyKey)
                        .build());
    }

    public CheckoutResponse createCheckoutSession(CheckoutRequest request) throws StripeException {

        // Build line items
        SessionCreateParams.LineItem.PriceData.ProductData productData =
                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName(request.getPlanName())
                        .build();

        SessionCreateParams.LineItem.PriceData priceData =
                SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency("usd")
                        .setUnitAmount((long) (request.getPrice() * 100)) // Convert to cents
                        .setProductData(productData)
                        .build();

        SessionCreateParams.LineItem lineItem =
                SessionCreateParams.LineItem.builder()
                        .setPriceData(priceData)
                        .setQuantity(1L)
                        .build();

        // Build session parameters
        SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT) // Use SUBSCRIPTION for recurring
                .setSuccessUrl(frontendUrl + "/stripe/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontendUrl + "/pricing?type=selfhosted")
                .addLineItem(lineItem)
                .putMetadata("planId", request.getPlanId());

        // Optionally pre-fill customer email
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            paramsBuilder.setCustomerEmail(request.getEmail());
        }

        SessionCreateParams params = paramsBuilder.build();

        // Create the session
        Session session = Session.create(params);

        return new CheckoutResponse(session.getId(), session.getUrl());
    }

    public Session retrieveSession(String sessionId) throws StripeException {
        return Session.retrieve(sessionId);
    }
}