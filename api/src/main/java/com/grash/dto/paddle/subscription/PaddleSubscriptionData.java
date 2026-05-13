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

    private String id;

    @Schema(description = "Subscription status")

    private PaddleSubscriptionStatus status;

    @Schema(description = "Customer ID")
    private String customerId;

    @Schema(description = "Billing details")
    private BillingDetails billingDetails;

    @Schema(description = "Custom key-value data")
    private Map<String, String> customData;

    @Schema(description = "Subscription items")

    private List<PaddleItem> items;

    @Schema(description = "Next billing date and time (ISO 8601)")
    private String nextBilledAt;

    @Schema(description = "Scheduled change details")
    private ScheduledChange scheduledChange;

    @Schema(description = "Current billing period")
    private BillingPeriod currentBillingPeriod;

}
