package com.grash.dto.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Complete license data including attributes, relationships, and links")
public class LicenseData {
    @Schema(description = "Unique identifier for the license")
    private String id;
    @Schema(description = "Resource type, typically 'licenses'")
    private String type;
    @Schema(description = "License attributes")
    private LicenseAttributes attributes;
    @Schema(description = "Relationships to related resources (account, product, etc.)")
    private LicenseRelationships relationships;
    @Schema(description = "HATEOAS links for the license resource")
    private Map<String, String> links;
}
