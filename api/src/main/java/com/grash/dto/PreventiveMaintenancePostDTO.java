package com.grash.dto;

import com.grash.model.abstracts.WorkOrderBase;
import com.grash.model.enums.RecurrenceBasedOn;
import com.grash.model.enums.RecurrenceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "DTO for creating a new preventive maintenance schedule")
public class PreventiveMaintenancePostDTO extends WorkOrderBase {
    @Schema(description = "Start date of the maintenance")
    private Date startsOn;

    @Schema(description = "Name")
    @NotNull
    private String name;

    @Schema(description = "Frequency of recurrence")
    @NotNull
    private int frequency;

    @Schema(description = "Due date delay")
    private Integer dueDateDelay;

    @Schema(description = "End date of the maintenance")
    private Date endsOn;

    @Schema(description = "Recurrence type")
    @NotNull
    private RecurrenceType recurrenceType;

    @Schema(description = "What the recurrence is based on")
    @NotNull
    private RecurrenceBasedOn recurrenceBasedOn;

    @Schema(description = "Days of week for recurrence")
    private List<Integer> daysOfWeek = new ArrayList<>();
}


