package com.grash.dto.paddle.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "Paddle subscription item")
public class PaddleItem {
    @Schema(description = "Price ID for the subscription item")

    private String priceId;

    @Schema(description = "Item quantity")

    private Integer quantity;

    @Schema(description = "Item status")

    private String status;


    private Map<String, String> customData;

}
