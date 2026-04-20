package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "DTO for patching a task option")
public class TaskOptionPatchDTO {
    @Schema(description = "Option label")
    private String label;
}
