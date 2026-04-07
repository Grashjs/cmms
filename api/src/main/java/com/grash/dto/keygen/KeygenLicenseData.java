package com.grash.dto.keygen;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Keygen license data wrapper for API requests")
public class KeygenLicenseData {
    @Schema(description = "Resource type (e.g., 'license')")
    private String type;
    @Schema(description = "License attributes")
    private KeygenLicenseAttributes attributes;
    @Schema(description = "License relationships to policy and user")
    private KeygenLicenseRelationships relationships;
}
