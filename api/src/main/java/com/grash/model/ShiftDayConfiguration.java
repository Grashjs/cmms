package com.grash.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShiftDayConfiguration {

    @Enumerated(EnumType.STRING)
    @Schema(description = "Day of the week")
    @NotNull
    private DayOfWeek dayOfWeek;

    @Schema(description = "Availability in minutes for this day")
    @Max(60 * 24)
    @Min(0)
    private int availabilityMinutes;

    private boolean enabled = true;
}
