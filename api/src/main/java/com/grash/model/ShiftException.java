package com.grash.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShiftException {

    @Schema(description = "The specific date for this exception")
    @NotNull
    private LocalDate exceptionDate;

    @Schema(description = "Availability in minutes for this date")
    @Max(60 * 24)
    @Min(0)
    private int availabilityMinutes;

    @Schema(description = "Whether this date is enabled")
    private boolean enabled = true;
}
