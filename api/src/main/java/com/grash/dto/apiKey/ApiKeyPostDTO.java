package com.grash.dto.apiKey;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "DTO for creating a new API key")
public class ApiKeyPostDTO {
    @Schema(description = "API key label")
    @NotNull
    private String label;
}
