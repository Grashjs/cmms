package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "DTO for patching a base task")
public class TaskBasePatchDTO {

    @Schema(description = "Task label")
    private String label;

}
