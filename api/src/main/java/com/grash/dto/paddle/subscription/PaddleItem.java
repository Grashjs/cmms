package com.grash.dto.paddle.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Paddle subscription item")
public class PaddleItem {
    @Schema(description = "Price ID for the subscription item")
    @JsonProperty("price_id")
    private String priceId;

    @Schema(description = "Item quantity")
    @JsonProperty("quantity")
    private Integer quantity;

    @Schema(description = "Item status")
    @JsonProperty("status")
    private String status;
}
