package com.grash.service;

import com.stripe.Stripe;
import com.stripe.model.Customer;
import com.stripe.model.Subscription;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.SubscriptionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeService {

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.plan-id}")
    private String stripePlanId;

    public Customer createCustomer(String email, String description, String stripeToken, String keygenUserId) throws Exception {
        Stripe.apiKey = stripeSecretKey;

        CustomerCreateParams params = CustomerCreateParams.builder()
                .setDescription(description)
                .setEmail(email)
                .setSource(stripeToken)
                .putMetadata("keygenUserId", keygenUserId)
                .build();

        return Customer.create(params);
    }

    public Subscription createSubscription(String customerId, String idempotencyKey) throws Exception {
        Stripe.apiKey = stripeSecretKey;

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
}