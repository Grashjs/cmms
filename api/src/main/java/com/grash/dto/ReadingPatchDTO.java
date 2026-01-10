package com.grash.dto;

import com.grash.model.Meter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ReadingPatchDTO {

    private Double value;

    @Schema(implementation = IdDTO.class)
    private Meter meter;
}
