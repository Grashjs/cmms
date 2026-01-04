package com.grash.dto.paddle.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PaymentTerms {
    @JsonProperty("interval")
    private String interval;

    @JsonProperty("frequency")
    private Integer frequency;
}
