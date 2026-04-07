package com.grash.dto;

import com.grash.model.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "DTO for patching an existing task")
public class TaskPatchDTO {

    @Schema(description = "Task status")
    private Status status;

    @Schema(description = "Task notes")
    private String notes;

    @Schema(description = "Task value")
    private String value;

}
