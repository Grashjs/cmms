package com.grash.dto.license;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Metadata for license validation requests containing scope information")
public class LicenseValidationMeta {
    @Schema(description = "Unique key identifier for the validation")
    private String key;
    @Schema(description = "Validation scope including fingerprint data")
    private LicenseValidationScope scope;
}
