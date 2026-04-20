package com.grash.dto;

import com.grash.model.Part;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@NoArgsConstructor
@Schema(description = "DTO for patching multiparts information")
public class MultiPartsPatchDTO {

    @Schema(description = "Name")
    private String name;

    @ArraySchema(schema = @Schema(implementation = IdDTO.class))
    @Schema(description = "Collection of parts")
    private Collection<Part> parts;
}
