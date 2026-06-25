package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "DTO for restocking a part")
public class PartRestockDTO {
    @Schema(description = "Quantity to add to stock (must be positive)")
    @NotNull
    @Min(value = 0L, message = "The value must be positive")
    private double quantity;

    @Schema(description = "Description of the restock operation")
    private String description;
}
