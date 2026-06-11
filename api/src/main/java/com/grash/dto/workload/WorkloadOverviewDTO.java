package com.grash.dto.workload;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "Top-level workload overview for a date range")
public class WorkloadOverviewDTO {
    @Schema(description = "Start date of the range")
    private LocalDate startDate;
    @Schema(description = "End date of the range")
    private LocalDate endDate;
    @Schema(description = "Total team capacity in minutes for the entire range")
    private int teamCapacityMinutes;
    @Schema(description = "Total team allocated time in minutes for the entire range")
    private double teamAllocatedMinutes;
    @Schema(description = "Per-day breakdown")
    private List<WorkloadDayDTO> days = new ArrayList<>();
}
