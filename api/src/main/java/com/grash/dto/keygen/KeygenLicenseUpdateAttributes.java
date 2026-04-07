package com.grash.dto.keygen;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "Keygen license update attributes")
public class KeygenLicenseUpdateAttributes {
    @Schema(description = "New expiry date for the license")
    private String expiry;
    @Schema(description = "Updated metadata key-value pairs")
    private Map<String, Object> metadata;
}