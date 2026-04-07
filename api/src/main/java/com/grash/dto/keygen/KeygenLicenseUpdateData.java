package com.grash.dto.keygen;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Keygen license update data wrapper")
public class KeygenLicenseUpdateData {
    @Schema(description = "Resource type (e.g., 'license')")
    private String type;
    @Schema(description = "Updated license attributes")
    private KeygenLicenseUpdateAttributes attributes;
}