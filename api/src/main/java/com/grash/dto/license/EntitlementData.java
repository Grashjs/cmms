package com.grash.dto.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "License entitlement data containing ID, type, and attributes")
public class EntitlementData {
    @Schema(description = "Unique identifier for the entitlement")
    private String id;
    @Schema(description = "Resource type, typically 'entitlements'")
    private String type;
    @Schema(description = "Attributes describing the entitlement")
    private EntitlementAttributes attributes;
}
