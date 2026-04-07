package com.grash.dto.fastSpring.payloads;

import com.grash.dto.fastSpring.Subscription;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "FastSpring subscription charge details")
public class SubscriptionCharge {
    @Schema(description = "Currency code for the charge")
    public String currency;
    @Schema(description = "Total charge amount")
    public int total;
    @Schema(description = "Charge status (e.g., success, pending)")
    public String status;
    @Schema(description = "Timestamp when the charge occurred")
    public long timestamp;
    @Schema(description = "Timestamp value")
    public long timestampValue;
    @Schema(description = "Timestamp in seconds since epoch")
    public int timestampInSeconds;
    @Schema(description = "Formatted timestamp display")
    public String timestampDisplay;
    @Schema(description = "Billing sequence number")
    public int sequence;
    @Schema(description = "Number of billing periods covered by this charge")
    public Integer periods;
    @Schema(description = "Quote reference for this charge")
    public String quote;
    @Schema(description = "Subscription details for this charge")
    public Subscription subscription;
}
