package com.grash.dto.imports;

import com.grash.model.enums.RecurrenceBasedOn;
import com.grash.model.enums.RecurrenceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@SuperBuilder
@AllArgsConstructor
@Schema(description = "DTO for importing preventive maintenance schedules from external data sources")
public class PreventiveMaintenanceImportDTO extends WorkOrderImportDTO {
    @Schema(description = "Start date (timestamp)")
    private Double startsOn;

    @Schema(description = "Name")
    @NotNull
    private String name;

    @Schema(description = "Frequency of recurrence")
    @NotNull
    private double frequency;

    @Schema(description = "Due date delay")
    private Double dueDateDelay;

    @Schema(description = "End date (timestamp)")
    private Double endsOn;

    @Schema(description = "Recurrence type")
    @NotNull
    private String recurrenceType;

    @Schema(description = "What the recurrence is based on")
    @NotNull
    private String recurrenceBasedOn;
    
    @Schema(description = "Days of week for recurrence")
    @Builder.Default
    private List<String> daysOfWeek = new ArrayList<>();
}

