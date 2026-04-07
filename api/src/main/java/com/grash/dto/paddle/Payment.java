package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Paddle payment record for a transaction")
public class Payment {
    @Schema(description = "Payment amount")
    private String amount;

    @Schema(description = "Payment status")
    private String status;

    @Schema(description = "Payment creation timestamp (ISO 8601)")
    @JsonProperty("created_at")
    private String createdAt;

    @Schema(description = "Error code if payment failed")
    @JsonProperty("error_code")
    private String errorCode;

    @Schema(description = "Timestamp when payment was captured (ISO 8601)")
    @JsonProperty("captured_at")
    private String capturedAt;

    @Schema(description = "Payment method details")
    @JsonProperty("method_details")
    private MethodDetails methodDetails;

    @Schema(description = "Payment attempt ID")
    @JsonProperty("payment_attempt_id")
    private String paymentAttemptId;

    @Schema(description = "Payment method ID")
    @JsonProperty("payment_method_id")
    private String paymentMethodId;

    @Schema(description = "Stored payment method ID")
    @JsonProperty("stored_payment_method_id")
    private String storedPaymentMethodId;
}
