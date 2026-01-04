package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TransactionItem {
    @JsonProperty("price_id")
    private String priceId;

    private Price price;

    private Integer quantity;

    private String proration;
}
