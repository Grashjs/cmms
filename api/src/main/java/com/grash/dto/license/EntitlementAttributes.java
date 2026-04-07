package com.grash.dto.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Attributes of a license entitlement, containing its name and code")
public class EntitlementAttributes {
    @Schema(description = "Display name of the entitlement")
    private String name;
    @Schema(description = "Unique code identifier for the entitlement")
    private String code;
    @Schema(description = "Timestamp when the entitlement was created")
    private String created;
    @Schema(description = "Timestamp when the entitlement was last updated")
    private String updated;
}
