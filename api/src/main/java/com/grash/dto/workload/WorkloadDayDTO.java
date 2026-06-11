package com.grash.dto.workload;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "Single day breakdown in workload overview")
public class WorkloadDayDTO {
    @Schema(description = "The date")
    private LocalDate date;
    @Schema(description = "Day of the week")
    private DayOfWeek dayOfWeek;
    @Schema(description = "Total team capacity in minutes for this day")
    private int teamCapacityMinutes;
    @Schema(description = "Total team allocated time in minutes for this day")
    private double teamAllocatedMinutes;
    @Schema(description = "Per-user breakdown for this day")
    private List<WorkloadUserDayDTO> users = new ArrayList<>();
}
