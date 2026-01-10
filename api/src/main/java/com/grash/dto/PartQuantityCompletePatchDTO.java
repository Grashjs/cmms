package com.grash.dto;

import com.grash.model.Part;
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

    @NotNull
    private Part part;
}

