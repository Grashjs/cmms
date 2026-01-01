package com.grash.dto.paddle;

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

    @JsonProperty("customer_name")
    private String customerName;

    @JsonProperty("company_number")
    private String companyNumber;

    @JsonProperty("tax_number")
    private String taxNumber;

    private Address address;
}
