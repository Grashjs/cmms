package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@Schema(description = "DTO for creating a new category")
public class CategoryPostDTO {

    @Schema(description = "Name")
    @NotNull
    private String name;

    @Schema(description = "Description")
    private String description;
}

