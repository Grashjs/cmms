package com.grash.dto.keygen;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Keygen user creation data wrapper")
public class KeygenUserCreateData {
    @Schema(description = "Resource type (e.g., 'user')")
    private String type;
    @Schema(description = "User creation attributes")
    private KeygenUserCreateAttributes attributes;
}
