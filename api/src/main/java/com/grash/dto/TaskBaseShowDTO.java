package com.grash.dto;

import com.grash.model.enums.TaskType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "DTO for displaying base task details in API responses")
public class TaskBaseShowDTO extends AuditShowDTO {
    @Schema(description = "Task label")
    private String label;

    @Schema(description = "Type of task")
    private TaskType taskType;

    @Schema(description = "List of task options")
    private List<TaskOptionShowDTO> options = new ArrayList<>();

    @Schema(description = "Assigned user")
    private UserMiniDTO user;

    @Schema(description = "Associated asset")
    private AssetMiniDTO asset;

    @Schema(description = "Associated meter")
    private MeterMiniDTO meter;
}
