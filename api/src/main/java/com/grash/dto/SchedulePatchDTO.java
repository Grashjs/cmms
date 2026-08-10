package com.grash.dto;

import com.grash.model.enums.RecurrenceBasedOn;
import com.grash.model.enums.RecurrenceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "DTO for patching a maintenance schedule")
public class SchedulePatchDTO {
    @Schema(description = "Whether the schedule is paused. A paused schedule generates no work orders")
    private Boolean disabled;

    @Schema(description = "Start date")
    private Date startsOn;

    @Schema(description = "Frequency of recurrence")
    private Integer frequency;

    @Schema(description = "End date")
    private Date endsOn;

    @Schema(description = "Due date delay")
    private Integer dueDateDelay;

    @Schema(description = "Recurrence type")
    private RecurrenceType recurrenceType;

    @Schema(description = "What the recurrence is based on")
    private RecurrenceBasedOn recurrenceBasedOn;

    @Schema(description = "Days of week for recurrence")
    private List<Integer> daysOfWeek;
}

