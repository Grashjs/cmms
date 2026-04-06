package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "DTO for displaying task option details in API responses")
public class TaskOptionShowDTO extends AuditShowDTO {
    @Schema(description = "Option label")
    private String label;
}
