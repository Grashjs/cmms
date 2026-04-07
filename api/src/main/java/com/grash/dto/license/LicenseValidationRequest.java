// Request DTOs
package com.grash.dto.license;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request payload for validating a software license")
public class LicenseValidationRequest {
    @Schema(description = "Validation metadata including scope and fingerprint")
    private LicenseValidationMeta meta;
}