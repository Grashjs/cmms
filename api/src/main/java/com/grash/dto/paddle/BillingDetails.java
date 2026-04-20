package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Paddle billing details for a transaction or subscription")
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
    @JsonProperty("customer_name")
    private String customerName;

    @Schema(description = "Company registration number")
    @JsonProperty("company_number")
    private String companyNumber;

    @Schema(description = "Tax identification number")
    @JsonProperty("tax_number")
    private String taxNumber;

    @Schema(description = "Billing address")
    private Address address;
}
