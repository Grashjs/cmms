package com.grash.service;

import com.grash.dto.license.SelfHostedPlan;
import com.grash.dto.stripe.CheckoutRequest;
import com.grash.dto.stripe.CheckoutResponse;
import com.grash.exception.CustomException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static com.grash.utils.Consts.selfHostedPlans;

@Service
public class StripeService {

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${frontend.url}")
    private String frontendUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    public CheckoutResponse createCheckoutSession(CheckoutRequest request) throws StripeException {
        SelfHostedPlan plan = (selfHostedPlans.stream()
                .filter(selfHostedPlan -> selfHostedPlan.getId().equals(request.getPlanId()))
                .findFirst().orElseThrow(() -> new CustomException("Plan not found", HttpStatus.BAD_REQUEST)));
        // Build line items
        SessionCreateParams.LineItem.PriceData.ProductData productData =
                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName(plan.getName()).build();

        SessionCreateParams.LineItem.PriceData priceData =
                SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency("usd")
                        .setUnitAmountDecimal(plan.getPricePerUser().multiply(BigDecimal.valueOf(100))) // Convert to
                        // cents
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
                .setSuccessUrl(frontendUrl + "/payment/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontendUrl + "/pricing?type=selfhosted")
                .addLineItem(lineItem)
                .putMetadata("planId", request.getPlanId());

        paramsBuilder.setCustomerEmail(request.getEmail().trim().toLowerCase());

        SessionCreateParams params = paramsBuilder.build();

        // Create the session
        Session session = Session.create(params);

        return new CheckoutResponse(session.getId(), session.getUrl());
    }

    public Session retrieveSession(String sessionId) throws StripeException {
        return Session.retrieve(sessionId);
    }
}