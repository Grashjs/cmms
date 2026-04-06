package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "DTO for displaying floor plan details in API responses")
public class FloorPlanShowDTO {
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;
    
    @Schema(description = "Name")
    private String name;

    @Schema(description = "Floor plan image")
    private FileShowDTO image;

    @Schema(description = "Area size")
    private long area;

}
