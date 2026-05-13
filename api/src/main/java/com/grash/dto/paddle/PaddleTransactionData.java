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

    private String customerId;

    @Schema(description = "Address ID")

    private String addressId;

    @Schema(description = "Business ID")

    private String businessId;

    @Schema(description = "Custom key-value data")

    private Map<String, String> customData;

    @Schema(description = "Three-letter currency code (e.g., USD)")

    private String currencyCode;

    @Schema(description = "Billing details")

    private BillingDetails billingDetails;

    @Schema(description = "Billing period")

    private BillingPeriod billingPeriod;

    @Schema(description = "Transaction items")

    private List<TransactionItem> items;

    @Schema(description = "Transaction details with totals")

    private TransactionDetails details;

    @Schema(description = "Payment records")

    private List<Payment> payments;

    @Schema(description = "Checkout information")

    private Checkout checkout;

    @Schema(description = "Transaction creation timestamp (ISO 8601)")

    private String createdAt;

    @Schema(description = "Transaction last updated timestamp (ISO 8601)")

    private String updatedAt;

    @Schema(description = "Timestamp when the transaction was billed (ISO 8601)")

    private String billedAt;

    @Schema(description = "Timestamp when the transaction was revised (ISO 8601)")

    private String revisedAt;

    @Schema(description = "Discount ID")

    private String discountId;

    @Schema(description = "Invoice ID")

    private String invoiceId;

    @Schema(description = "Invoice number")

    private String invoiceNumber;

    @Schema(description = "Collection mode (e.g., automatic, manual)")

    private String collectionMode;

    @Schema(description = "Associated subscription ID")

    private String subscriptionId;

    @Schema(description = "Receipt data")

    private String receiptData;

    // Helper method to get customer email from address
    public String getCustomerEmail() {
        // Email is not directly in the transaction data in the provided JSON
        // You may need to fetch it from the customer or address endpoint
        return null;
    }
}
