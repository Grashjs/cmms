package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PaddleTransactionData {
    private String id;

    private String status;

    private String origin;

    @JsonProperty("customer_id")
    private String customerId;

    @JsonProperty("address_id")
    private String addressId;

    @JsonProperty("business_id")
    private String businessId;

    @JsonProperty("custom_data")
    private Map<String, String> customData;

    @JsonProperty("currency_code")
    private String currencyCode;

    @JsonProperty("billing_details")
    private BillingDetails billingDetails;

    @JsonProperty("billing_period")
    private BillingPeriod billingPeriod;

    @JsonProperty("items")
    private List<TransactionItem> items;

    @JsonProperty("details")
    private TransactionDetails details;

    @JsonProperty("payments")
    private List<Payment> payments;

    @JsonProperty("checkout")
    private Checkout checkout;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    @JsonProperty("billed_at")
    private String billedAt;

    @JsonProperty("revised_at")
    private String revisedAt;

    @JsonProperty("discount_id")
    private String discountId;

    @JsonProperty("invoice_id")
    private String invoiceId;

    @JsonProperty("invoice_number")
    private String invoiceNumber;

    @JsonProperty("collection_mode")
    private String collectionMode;

    @JsonProperty("subscription_id")
    private String subscriptionId;

    @JsonProperty("receipt_data")
    private String receiptData;

    // Helper method to get customer email from address
    public String getCustomerEmail() {
        // Email is not directly in the transaction data in the provided JSON
        // You may need to fetch it from the customer or address endpoint
        return null;
    }
}
