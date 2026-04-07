package com.grash.dto.keygen;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Keygen user update data wrapper")
public class KeygenUserUpdateData {
    @Schema(description = "Resource type (e.g., 'user')")
    private String type;
    @Schema(description = "Updated user attributes")
    private KeygenUserUpdateAttributes attributes;
}
