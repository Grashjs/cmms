package com.grash.dto.apiKey;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "DTO for patching an API key")
public class ApiKeyPatchDTO {
    @Schema(description = "API key label")
    @NotNull
    private String label;
}
