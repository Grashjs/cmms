package com.grash.dto;

import com.grash.model.File;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "DTO for patching an existing floor plan")
public class FloorPlanPatchDTO {

    @Schema(description = "Name")
    private String name;

    @Schema(description = "Floor plan image", implementation = IdDTO.class)
    private File image;

    @Schema(description = "Area size")
    private long area;
}
