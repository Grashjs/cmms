package com.grash.dto.keygen;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Keygen license API response wrapper")
public class KeygenLicenseResponse {
    @Schema(description = "License response data")
    private KeygenLicenseResponseData data;
    @Schema(description = "Error information if the request failed")
    private Object errors;
}
