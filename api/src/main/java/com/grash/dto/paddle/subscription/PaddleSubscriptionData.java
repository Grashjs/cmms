package com.grash.dto.paddle.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.dto.paddle.BillingDetails;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Schema(description = "Paddle subscription data from webhook events")
public class PaddleSubscriptionData {
    @Schema(description = "Subscription ID")
    @JsonProperty("id")
    private String id;

    @Schema(description = "Subscription status")
    @JsonProperty("status")
    private PaddleSubscriptionStatus status;

    @Schema(description = "Customer ID")
    @JsonProperty("customer_id")
    private String customerId;

    @Schema(description = "Billing details")
    @JsonProperty("billing_details")
    private BillingDetails billingDetails;

    @Schema(description = "Custom key-value data")
    @JsonProperty("custom_data")
    private Map<String, String> customData;

    @Schema(description = "Subscription items")
    @JsonProperty("items")
    private List<PaddleItem> items;

    @Schema(description = "Next billing date and time (ISO 8601)")
    @JsonProperty("next_billed_at")
    private String nextBilledAt;

    @Schema(description = "Scheduled change details")
    @JsonProperty("scheduled_change")
    private ScheduledChange scheduledChange;

    @Schema(description = "Current billing period")
    @JsonProperty("current_billing_period")
    private BillingPeriod currentBillingPeriod;

}
