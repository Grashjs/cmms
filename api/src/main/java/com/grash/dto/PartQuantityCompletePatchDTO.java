package com.grash.dto;

import com.grash.model.Part;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@Schema(description = "DTO for patching complete part quantity information")
public class PartQuantityCompletePatchDTO {
    @Schema(description = "Quantity value (must be positive)")
    @NotNull
    @Min(value = 0L, message = "The value must be positive")
    private double quantity;

    @Schema(description = "Part reference", implementation = IdDTO.class)
    @NotNull
    private Part part;
}

