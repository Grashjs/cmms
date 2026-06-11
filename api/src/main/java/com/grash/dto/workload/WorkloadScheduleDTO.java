package com.grash.dto.workload;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@Schema(description = "DTO for scheduling a work order from the workload view")
public class WorkloadScheduleDTO {
    @Schema(description = "Estimated start date (null to unschedule)")
    private Date estimatedStartDate;
    @Schema(description = "Estimated duration in hours")
    private Double estimatedDuration;
    @Schema(description = "User ID to assign as primary user / assignee")
    private Long primaryUserId;
}
