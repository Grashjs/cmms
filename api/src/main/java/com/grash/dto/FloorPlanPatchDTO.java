package com.grash.dto;

import com.grash.model.File;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FloorPlanPatchDTO {

    private String name;

    @Schema(implementation = IdDTO.class)
    private File image;

    private long area;
}
