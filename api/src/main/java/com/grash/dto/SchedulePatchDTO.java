package com.grash.dto;

import com.grash.model.enums.RecurrenceBasedOn;
import com.grash.model.enums.RecurrenceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "DTO for patching a maintenance schedule")
public class SchedulePatchDTO {
    @Schema(description = "Start date")
    private Date startsOn;

    @Schema(description = "Frequency of recurrence")
    private int frequency;

    @Schema(description = "End date")
    private Date endsOn;

    @Schema(description = "Due date delay")
    private Integer dueDateDelay;

    @Schema(description = "Recurrence type")
    @NotNull
    private RecurrenceType recurrenceType;

    @Schema(description = "What the recurrence is based on")
    @NotNull
    private RecurrenceBasedOn recurrenceBasedOn;

    @Schema(description = "Days of week for recurrence")
    private List<Integer> daysOfWeek = new ArrayList<>();
}

