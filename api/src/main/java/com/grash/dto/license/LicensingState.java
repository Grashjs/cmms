package com.grash.dto.license;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
@Schema(description = "Current licensing state for an organization, including plan, entitlements, and validity")
public class LicensingState {
    @Schema(description = "Whether the organization has an active license")
    private boolean hasLicense;
    @Schema(description = "Whether the current license is valid")
    private boolean valid;
    @Schema(description = "Name of the current subscription plan")
    private String planName;
    @Schema(description = "Set of entitlement codes granted by the current license")
    @Builder.Default
    private Set<String> entitlements = new HashSet<>();
    @Schema(description = "Expiration date of the current license")
    private Date expirationDate;
    @Schema(description = "Number of licensed users")
    private int usersCount;
}
