package com.grash.dto.keygen;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Keygen license attributes for creating or updating a license")
public class KeygenLicenseAttributes {
    @Schema(description = "License name or identifier")
    private String name;
    @Schema(description = "Custom metadata key-value pairs")
    private Map<String, String> metadata;
}
