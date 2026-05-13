package com.grash.dto.paddle.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class PaddlePrice {
    @JsonProperty("id")
    private String id;

    @JsonProperty("custom_data")
    private Map<String, String> customData;
}
