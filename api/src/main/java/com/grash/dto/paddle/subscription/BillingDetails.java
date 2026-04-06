package com.grash.dto.paddle.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Paddle subscription billing details")
public class BillingDetails {
    @Schema(description = "Whether checkout is enabled")
    @JsonProperty("enable_checkout")
    private Boolean enableCheckout;

    @Schema(description = "Payment terms configuration")
    @JsonProperty("payment_terms")
    private PaymentTerms paymentTerms;

    @Schema(description = "Purchase order number")
    @JsonProperty("purchase_order_number")
    private String purchaseOrderNumber;

    @Schema(description = "Additional billing information")
    @JsonProperty("additional_information")
    private String additionalInformation;

    @Schema(description = "Customer name")
    @JsonProperty("name")
    private String customerName;
}
