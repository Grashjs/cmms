package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BillingPeriod {
    @JsonProperty("starts_at")
    private String startsAt;

    @JsonProperty("ends_at")
    private String endsAt;
}
