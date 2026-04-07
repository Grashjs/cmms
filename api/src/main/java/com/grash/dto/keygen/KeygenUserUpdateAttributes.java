package com.grash.dto.keygen;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Keygen user update attributes")
public class KeygenUserUpdateAttributes {
    @Schema(description = "Updated metadata key-value pairs")
    private Map<String, String> metadata;
}
