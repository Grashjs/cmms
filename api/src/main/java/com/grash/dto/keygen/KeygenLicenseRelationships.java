package com.grash.dto.keygen;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Keygen license relationships to policy and user resources")
public class KeygenLicenseRelationships {
    @Schema(description = "Relationship to the licensing policy")
    private KeygenRelationship policy;
    @Schema(description = "Relationship to the license user")
    private KeygenRelationship user;
}
