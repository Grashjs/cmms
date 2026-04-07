package com.grash.dto.license;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Scope of a license validation request, including machine fingerprint")
public class LicenseValidationScope {
    @Schema(description = "Machine fingerprint for validation binding")
    private String fingerprint;
}
