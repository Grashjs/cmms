package com.grash.dto.keygen;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Keygen user resource")
public class KeygenUser {
    @Schema(description = "User unique identifier")
    private String id;
    @Schema(description = "Resource type (e.g., 'user')")
    private String type;
    @Schema(description = "User attributes and details")
    private KeygenUserAttributes attributes;
}
