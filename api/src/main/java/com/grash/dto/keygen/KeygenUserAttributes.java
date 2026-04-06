package com.grash.dto.keygen;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Keygen user attributes")
public class KeygenUserAttributes {
    @Schema(description = "User email address")
    private String email;
    @Schema(description = "Custom metadata key-value pairs")
    private Map<String, Object> metadata;
}
