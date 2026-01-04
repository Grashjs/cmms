package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Payment {
    private String amount;
    private String status;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("error_code")
    private String errorCode;

    @JsonProperty("captured_at")
    private String capturedAt;

    @JsonProperty("method_details")
    private MethodDetails methodDetails;

    @JsonProperty("payment_attempt_id")
    private String paymentAttemptId;

    @JsonProperty("payment_method_id")
    private String paymentMethodId;

    @JsonProperty("stored_payment_method_id")
    private String storedPaymentMethodId;
}
