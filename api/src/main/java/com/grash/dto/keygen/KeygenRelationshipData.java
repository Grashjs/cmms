package com.grash.dto.keygen;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Keygen related resource reference data")
public class KeygenRelationshipData {
    @Schema(description = "Resource type (e.g., 'policy', 'user')")
    private String type;
    @Schema(description = "Resource unique identifier")
    private String id;
}
