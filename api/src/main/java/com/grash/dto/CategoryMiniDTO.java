package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryMiniDTO {
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;
    
    @Schema(description = "Name")
    private String name;

    // A category name is often only a code (a DIN 276 class, say), so the
    // description carries the readable part and detail views need it.
    @Schema(description = "Description")
    private String description;
}
