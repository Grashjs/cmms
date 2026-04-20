package com.grash.dto.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Response containing a list of license entitlements")
public class EntitlementsResponse {
    @Schema(description = "List of entitlement data objects")
    private List<EntitlementData> data;
}
