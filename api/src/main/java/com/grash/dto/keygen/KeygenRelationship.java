package com.grash.dto.keygen;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Keygen relationship to a related resource")
public class KeygenRelationship {
    @Schema(description = "Related resource data (type and id)")
    private KeygenRelationshipData data;
}
