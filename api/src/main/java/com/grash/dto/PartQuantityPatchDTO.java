package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@Schema(description = "DTO for patching part quantity")
public class PartQuantityPatchDTO {
    @Schema(description = "Quantity value (must be positive)")
    @NotNull
    @Min(value = 0L, message = "The value must be positive")
    private double quantity;
}

