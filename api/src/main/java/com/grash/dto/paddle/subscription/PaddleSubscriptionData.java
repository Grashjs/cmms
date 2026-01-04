package com.grash.dto.paddle.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.dto.paddle.BillingDetails;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PaddleSubscriptionData {
    @JsonProperty("id")
    private String id;

    @JsonProperty("status")
    private String status;

    @JsonProperty("customer_id")
    private String customerId;

    @JsonProperty("subscription_id")
    private String subscriptionId;

    @JsonProperty("billing_details")
    private BillingDetails billingDetails;

    @JsonProperty("custom_data")
    private Map<String, String> customData;

    @JsonProperty("items")
    private List<PaddleItem> items;

    @JsonProperty("next_billed_at")
    private String nextBilledAt;

    @JsonProperty("scheduled_change")
    private ScheduledChange scheduledChange;

    @JsonProperty("current_billing_period")
    private BillingPeriod currentBillingPeriod;

}
