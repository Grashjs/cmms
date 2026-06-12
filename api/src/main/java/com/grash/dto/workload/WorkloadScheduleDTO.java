package com.grash.dto.workload;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Date;

@Data
@NoArgsConstructor
@Schema(description = "DTO for scheduling a work order from the workload view")
public class WorkloadScheduleDTO {
    private LocalDate localDate;
    @Schema(description = "User ID to assign as primary user / assignee")
    private Long primaryUserId;
}
