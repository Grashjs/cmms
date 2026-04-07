package com.grash.dto.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Scope of license validation containing the machine fingerprint")
public class ValidationScope {
    @Schema(description = "Machine fingerprint used for validation binding")
    private String fingerprint;
}
