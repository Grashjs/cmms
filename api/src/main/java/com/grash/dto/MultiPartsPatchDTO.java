package com.grash.dto;

import com.grash.model.Part;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@NoArgsConstructor
public class MultiPartsPatchDTO {

    private String name;

    @ArraySchema(schema = @Schema(implementation = IdDTO.class))
    private Collection<Part> parts;
}
