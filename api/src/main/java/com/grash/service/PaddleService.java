package com.grash.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.dto.license.SelfHostedPlan;
import com.grash.dto.checkout.CheckoutRequest;
import com.grash.dto.checkout.CheckoutResponse;
import com.grash.exception.CustomException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.*;

import static com.grash.utils.Consts.selfHostedPlans;

@Service
@RequiredArgsConstructor
public class PaddleService {

    @Value("${paddle.api-key}")
    private String paddleApiKey;

    @Value("${paddle.environment:sandbox}")
    private String paddleEnvironment;

    @Value("${frontend.url}")
    private String frontendUrl;

    private String paddleApiUrl;
    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void init() {
        // Paddle API base URL
        this.paddleApiUrl = "sandbox".equalsIgnoreCase(paddleEnvironment)
                ? "https://sandbox-api.paddle.com"
                : "https://api.paddle.com";
    }

    public CheckoutResponse createCheckoutSession(CheckoutRequest request) {
        SelfHostedPlan plan = selfHostedPlans.stream()
                .filter(selfHostedPlan -> selfHostedPlan.getId().equals(request.getPlanId()))
                .findFirst()
                .orElseThrow(() -> new CustomException("Plan not found", HttpStatus.BAD_REQUEST));

        PaddleTransactionRequest transactionRequest = new PaddleTransactionRequest();

        // Add item
        PaddleItem item = new PaddleItem();
        item.setPriceId(plan.getPaddlePriceId());
        item.setQuantity(1);
        transactionRequest.setItems(Collections.singletonList(item));

        // Set customer email
        transactionRequest.setCustomerEmail(request.getEmail().trim().toLowerCase());

        // Set custom data (metadata)
        Map<String, String> customData = new HashMap<>();
        customData.put("planId", request.getPlanId());
        customData.put("email", request.getEmail().trim().toLowerCase());
        transactionRequest.setCustomData(customData);

        // Set checkout settings
        PaddleCheckout checkout = new PaddleCheckout();
//        checkout.setUrl(frontendUrl + "/payment/success?_ptxn={transaction_id}");
        transactionRequest.setCheckout(checkout);

        try {
            // Create HTTP headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(paddleApiKey);

            HttpEntity<PaddleTransactionRequest> entity = new HttpEntity<>(transactionRequest, headers);

            // Make API call
            ResponseEntity<PaddleTransactionResponse> response = restTemplate.exchange(
                    paddleApiUrl + "/transactions",
                    HttpMethod.POST,
                    entity,
                    PaddleTransactionResponse.class
            );

            if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                PaddleTransactionData data = response.getBody().getData();
                return new CheckoutResponse(data.getId());
            } else {
                throw new CustomException("Failed to create Paddle checkout session", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            throw new CustomException("Error creating Paddle checkout: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public PaddleTransactionData retrieveTransaction(String transactionId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(paddleApiKey);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<PaddleTransactionResponse> response = restTemplate.exchange(
                    paddleApiUrl + "/transactions/" + transactionId,
                    HttpMethod.GET,
                    entity,
                    PaddleTransactionResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody().getData();
            } else {
                throw new CustomException("Failed to retrieve transaction", HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            throw new CustomException("Error retrieving Paddle transaction: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Request DTOs
    @Data
    private static class PaddleTransactionRequest {
        private List<PaddleItem> items;

        @JsonProperty("customer_email")
        private String customerEmail;

        @JsonProperty("custom_data")
        private Map<String, String> customData;

        private PaddleCheckout checkout;
    }

    @Data
    private static class PaddleItem {
        @JsonProperty("price_id")
        private String priceId;

        private Integer quantity;
    }

    @Data
    private static class PaddleCheckout {
        private String url;
    }

    // Response DTOs
    @Data
    private static class PaddleTransactionResponse {
        private PaddleTransactionData data;
    }

    @Data
    public static class PaddleTransactionData {
        private String id;

        private String status;

        @JsonProperty("customer_id")
        private String customerId;

        @JsonProperty("custom_data")
        private Map<String, String> customData;

        @JsonProperty("created_at")
        private String createdAt;

        @JsonProperty("updated_at")
        private String updatedAt;
    }
}