package com.grash.dto.keygen;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Keygen license creation API request")
public class KeygenLicenseRequest {
    @Schema(description = "License data for the request")
    private KeygenLicenseData data;
}

