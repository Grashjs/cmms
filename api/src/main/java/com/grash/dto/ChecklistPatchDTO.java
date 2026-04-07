package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "DTO for patching an existing checklist")
public class ChecklistPatchDTO {
    @Schema(description = "Name")
    private String name;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "List of task bases associated with the checklist")
    private List<TaskBaseDTO> taskBases;

    @Schema(description = "Category")
    private String category;

}
