package com.grash.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.dto.license.SelfHostedPlan;
import com.grash.dto.checkout.CheckoutRequest;
import com.grash.dto.checkout.CheckoutResponse;
import com.grash.exception.CustomException;
import com.grash.model.OwnUser;
import com.grash.model.Subscription;
import com.grash.model.SubscriptionPlan;
import com.grash.model.enums.PlanFeatures;
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

    private final SubscriptionPlanService subscriptionPlanService;
    private final WorkflowService workflowService;
    private final UserService userService;

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
        if (request.getUserId() == null && request.getEmail() == null)
            throw new CustomException("Email and ID cannot be null", HttpStatus.BAD_REQUEST);
        boolean selfHosted = request.getUserId() == null;

        PaddleTransactionRequest transactionRequest = new PaddleTransactionRequest();

        // Add item
        PaddleItem item = new PaddleItem();
        if (selfHosted) {
            SelfHostedPlan selfHostedPlan = selfHostedPlans.stream()
                    .filter(selfHostedPlan1 -> selfHostedPlan1.getId().equals(request.getPlanId()))
                    .findFirst()
                    .orElseThrow(() -> new CustomException("Plan not found", HttpStatus.BAD_REQUEST));
            item.setPriceId(selfHostedPlan.getPaddlePriceId());
            item.setQuantity(1);
        } else {
            SubscriptionPlan subscriptionPlan =
                    subscriptionPlanService.findByCode(request.getPlanId().split("-")[0].toUpperCase())
                            .orElseThrow(() -> new CustomException("Plan not found", HttpStatus.BAD_REQUEST));
            item.setPriceId(request.getPlanId().toLowerCase().contains("monthly") ?
                    subscriptionPlan.getMonthlyPaddlePriceId() : subscriptionPlan.getYearlyPaddlePriceId());
            item.setQuantity(request.getQuantity());
        }
        transactionRequest.setItems(Collections.singletonList(item));

        // Set customer email
        String email = (request.getUserId() == null ? request.getEmail() :
                userService.findById(request.getUserId()).get().getEmail()).trim().toLowerCase();
        transactionRequest.setCustomerEmail(email);

        // Set custom data (metadata)
        Map<String, String> customData = new HashMap<>();
        customData.put("planId", request.getPlanId());
        if (request.getUserId() != null) customData.put("userId", request.getUserId().toString());
        customData.put("email", email);
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

    public void updateSubscription(Subscription savedSubscription, String planCode, String paddleSubscriptionId,
                                   Date startsOn,
                                   Date endsOn, Long companyId, int usersCount) {
        boolean monthly = planCode.toLowerCase().contains("monthly");

        Collection<OwnUser> companyUsers = userService.findByCompany(companyId);

        int subscriptionUsersCount = (int) companyUsers.stream()
                .filter(OwnUser::isEnabledInSubscriptionAndPaid)
                .count();

        int enabledPaidUsersCount = (int) companyUsers.stream()
                .filter(user -> user.isEnabled() && user.isEnabledInSubscription() && user.getRole().isPaid())
                .count();
        
        if (enabledPaidUsersCount < subscriptionUsersCount) {
            savedSubscription.setDowngradeNeeded(true);
            savedSubscription.setUpgradeNeeded(false);
        } else if (enabledPaidUsersCount > subscriptionUsersCount) {
            savedSubscription.setUpgradeNeeded(true);
            savedSubscription.setDowngradeNeeded(false);
        } else {
            savedSubscription.setUpgradeNeeded(false);
            savedSubscription.setDowngradeNeeded(false);
        }

        savedSubscription.setMonthly(monthly);
        savedSubscription.setActivated(true);
        SubscriptionPlan subscriptionPlan =
                subscriptionPlanService.findByCode(planCode.split("-")[0].toUpperCase()).get();
        if (subscriptionPlan.getFeatures().contains(PlanFeatures.WORKFLOW)) {
            workflowService.enableWorkflows(companyId);
        } else {
            workflowService.disableWorkflows(companyId);
        }
        savedSubscription.setPaddleSubscriptionId(paddleSubscriptionId);
        savedSubscription.setSubscriptionPlan(subscriptionPlan);
        savedSubscription.setStartsOn(startsOn);
        savedSubscription.setEndsOn(endsOn);
        //avoid setting scheduledDate fields
        savedSubscription.setUsersCount(usersCount);
    }

    public void pauseSubscription(String subscriptionId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(paddleApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        restTemplate.exchange(
                paddleApiUrl + "/subscriptions/" + subscriptionId + "/pause",
                HttpMethod.POST,
                entity,
                Object.class
        );
    }

    public void resumeSubscription(String subscriptionId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(paddleApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = new HashMap<>();
        body.put("effective_from", "immediately");
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        restTemplate.exchange(
                paddleApiUrl + "/subscriptions/" + subscriptionId + "/resume",
                HttpMethod.POST,
                entity,
                Object.class
        );
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