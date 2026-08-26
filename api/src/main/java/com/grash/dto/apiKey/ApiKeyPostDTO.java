package com.grash.dto.apiKey;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "DTO for creating a new API key")
public class ApiKeyPostDTO {
    @Schema(description = "API key label")
    @NotNull
    private String label;

    @Schema(description = "Expiration date of the API key (optional)")
    private Date expiresAt;
}
