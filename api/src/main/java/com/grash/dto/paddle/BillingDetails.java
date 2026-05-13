package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Paddle billing details for a transaction or subscription")
public class BillingDetails {
    @Schema(description = "Whether checkout is enabled")
    private Boolean enableCheckout;

    @Schema(description = "Payment terms configuration")
    private PaymentTerms paymentTerms;

    @Schema(description = "Purchase order number")
    private String purchaseOrderNumber;

    @Schema(description = "Additional billing information")
    private String additionalInformation;

    @Schema(description = "Customer name")
    private String customerName;

    @Schema(description = "Company registration number")
    private String companyNumber;

    @Schema(description = "Tax identification number")
    private String taxNumber;

    @Schema(description = "Billing address")
    private Address address;
}
