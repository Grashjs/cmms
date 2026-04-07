package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@Schema(description = "DTO for patching an existing category")
public class CategoryPatchDTO {
    @Schema(description = "Name")
    @NotNull
    private String name;
    
    @Schema(description = "Description")
    private String description;
}

