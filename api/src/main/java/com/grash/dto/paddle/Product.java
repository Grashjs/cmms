package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class Product {
    private String id;
    private String name;
    private String type;
    private String status;
    private String description;

    @JsonProperty("image_url")
    private String imageUrl;

    @JsonProperty("tax_category")
    private String taxCategory;

    @JsonProperty("custom_data")
    private Map<String, String> customData;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;
}
