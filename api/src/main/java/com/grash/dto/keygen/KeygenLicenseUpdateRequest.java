package com.grash.dto.keygen;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Keygen license update API request")
public class KeygenLicenseUpdateRequest {
    @Schema(description = "License update data")
    private KeygenLicenseUpdateData data;
}