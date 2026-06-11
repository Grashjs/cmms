package com.grash.dto.workload;

import com.grash.model.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@Schema(description = "Lightweight work order DTO for workload view")
public class WorkloadWorkOrderDTO {
    @Schema(description = "Unique identifier of the work order")
    private Long id;
    @Schema(description = "Custom ID for the work order")
    private String customId;
    @Schema(description = "Title of the work order")
    private String title;
    @Schema(description = "Current status of the work order")
    private Status status;
    @Schema(description = "Estimated duration to complete (in hours)")
    private double estimatedDuration;
    @Schema(description = "Estimated start date")
    private Date estimatedStartDate;
    @Schema(description = "Due date")
    private Date dueDate;
}
