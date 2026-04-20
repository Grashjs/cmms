package com.grash.dto.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Response from license validation containing license data and validation metadata")
public class LicenseValidationResponse {
    @Schema(description = "Validated license data")
    private LicenseData data;
    @Schema(description = "Validation metadata including validity status and details")
    private ValidationMeta meta;
}
