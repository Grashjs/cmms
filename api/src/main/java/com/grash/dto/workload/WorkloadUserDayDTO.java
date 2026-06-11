package com.grash.dto.workload;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "Per-user breakdown for a single day in workload view")
public class WorkloadUserDayDTO {
    @Schema(description = "User ID")
    private Long userId;
    @Schema(description = "User full name")
    private String fullName;
    @Schema(description = "Available capacity in minutes for this day")
    private int capacityMinutes;
    @Schema(description = "Allocated time in minutes for this day")
    private double allocatedMinutes;
    @Schema(description = "Work orders scheduled on this day for this user")
    private List<WorkloadWorkOrderDTO> workOrders = new ArrayList<>();
}
