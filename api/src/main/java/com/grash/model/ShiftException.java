package com.grash.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Embeddable;
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
    private LocalDate exceptionDate;

    @Schema(description = "Availability in minutes for this date")
    private int availabilityMinutes;
}
