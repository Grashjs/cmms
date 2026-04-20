package com.grash.dto.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Validation metadata returned after license validation")
public class ValidationMeta {
    @Schema(description = "Whether the license validation was successful")
    private boolean valid;
    @Schema(description = "Detailed message explaining the validation result")
    private String detail;
    @Schema(description = "Error or status code from the validation")
    private String code;
    @Schema(description = "Timestamp when the validation was performed")
    private String ts;
    @Schema(description = "Scope of the validation including machine fingerprint")
    private ValidationScope scope;
}
