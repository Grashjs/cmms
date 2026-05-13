package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "Paddle product object")
public class Product {
    @Schema(description = "Product ID")
    private String id;

    @Schema(description = "Product name")
    private String name;

    @Schema(description = "Product type (e.g., standard, custom)")
    private String type;

    @Schema(description = "Product status")
    private String status;

    @Schema(description = "Product description")
    private String description;

    @Schema(description = "Product image URL")

    private String imageUrl;

    @Schema(description = "Tax category for the product")

    private String taxCategory;

    @Schema(description = "Custom key-value data")

    private Map<String, String> customData;

    @Schema(description = "Product creation timestamp (ISO 8601)")

    private String createdAt;

    @Schema(description = "Product last updated timestamp (ISO 8601)")

    private String updatedAt;
}
