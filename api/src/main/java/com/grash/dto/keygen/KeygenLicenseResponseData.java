package com.grash.dto.keygen;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Keygen license response data wrapper")
public class KeygenLicenseResponseData {
    @Schema(description = "License unique identifier")
    private String id;
    @Schema(description = "Resource type")
    private String type;
    @Schema(description = "License attributes and details")
    private KeygenLicenseResponseAttributes attributes;
    @Schema(description = "License relationships to policy and user")
    private KeygenLicenseRelationships relationships;
}
