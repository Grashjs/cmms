package com.grash.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
public class PartQuantityPatchDTO {
    @NotNull
    @Min(value = 0L, message = "The value must be positive")
    private double quantity;
}

