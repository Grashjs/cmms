package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Schema(description = "Paddle transaction data from webhook events")
public class PaddleTransactionData {
    @Schema(description = "Transaction ID")
    private String id;

    @Schema(description = "Transaction status")
    private String status;

    @Schema(description = "Transaction origin")
    private String origin;

    @Schema(description = "Customer ID")
    @JsonProperty("customer_id")
    private String customerId;

    @Schema(description = "Address ID")
    @JsonProperty("address_id")
    private String addressId;

    @Schema(description = "Business ID")
    @JsonProperty("business_id")
    private String businessId;

    @Schema(description = "Custom key-value data")
    @JsonProperty("custom_data")
    private Map<String, String> customData;

    @Schema(description = "Three-letter currency code (e.g., USD)")
    @JsonProperty("currency_code")
    private String currencyCode;

    @Schema(description = "Billing details")
    @JsonProperty("billing_details")
    private BillingDetails billingDetails;

    @Schema(description = "Billing period")
    @JsonProperty("billing_period")
    private BillingPeriod billingPeriod;

    @Schema(description = "Transaction items")
    @JsonProperty("items")
    private List<TransactionItem> items;

    @Schema(description = "Transaction details with totals")
    @JsonProperty("details")
    private TransactionDetails details;

    @Schema(description = "Payment records")
    @JsonProperty("payments")
    private List<Payment> payments;

    @Schema(description = "Checkout information")
    @JsonProperty("checkout")
    private Checkout checkout;

    @Schema(description = "Transaction creation timestamp (ISO 8601)")
    @JsonProperty("created_at")
    private String createdAt;

    @Schema(description = "Transaction last updated timestamp (ISO 8601)")
    @JsonProperty("updated_at")
    private String updatedAt;

    @Schema(description = "Timestamp when the transaction was billed (ISO 8601)")
    @JsonProperty("billed_at")
    private String billedAt;

    @Schema(description = "Timestamp when the transaction was revised (ISO 8601)")
    @JsonProperty("revised_at")
    private String revisedAt;

    @Schema(description = "Discount ID")
    @JsonProperty("discount_id")
    private String discountId;

    @Schema(description = "Invoice ID")
    @JsonProperty("invoice_id")
    private String invoiceId;

    @Schema(description = "Invoice number")
    @JsonProperty("invoice_number")
    private String invoiceNumber;

    @Schema(description = "Collection mode (e.g., automatic, manual)")
    @JsonProperty("collection_mode")
    private String collectionMode;

    @Schema(description = "Associated subscription ID")
    @JsonProperty("subscription_id")
    private String subscriptionId;

    @Schema(description = "Receipt data")
    @JsonProperty("receipt_data")
    private String receiptData;

    // Helper method to get customer email from address
    public String getCustomerEmail() {
        // Email is not directly in the transaction data in the provided JSON
        // You may need to fetch it from the customer or address endpoint
        return null;
    }
}
