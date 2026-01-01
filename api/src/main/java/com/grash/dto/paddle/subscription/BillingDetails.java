package com.grash.dto.paddle.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BillingDetails {
    @JsonProperty("enable_checkout")
    private Boolean enableCheckout;

    @JsonProperty("payment_terms")
    private PaymentTerms paymentTerms;

    @JsonProperty("purchase_order_number")
    private String purchaseOrderNumber;

    @JsonProperty("additional_information")
    private String additionalInformation;

    // For customer name - Paddle might send this as part of address or customer object
    @JsonProperty("name")
    private String customerName;
}
