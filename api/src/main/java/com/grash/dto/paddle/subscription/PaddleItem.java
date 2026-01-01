package com.grash.dto.paddle.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PaddleItem {
    @JsonProperty("price_id")
    private String priceId;

    @JsonProperty("quantity")
    private Integer quantity;

    @JsonProperty("status")
    private String status;
}
