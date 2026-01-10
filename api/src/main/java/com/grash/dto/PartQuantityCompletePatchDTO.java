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
public class PartQuantityCompletePatchDTO {
    @NotNull
    @Min(value = 0L, message = "The value must be positive")
    private double quantity;

    @Schema(implementation = IdDTO.class)
    @NotNull
    private Part part;
}

