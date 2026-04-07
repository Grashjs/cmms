package com.grash.dto;

import com.grash.model.Meter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "DTO for patching a meter reading")
public class ReadingPatchDTO {

    @Schema(description = "Reading value")
    private Double value;

    @Schema(description = "Meter reference", implementation = IdDTO.class)
    private Meter meter;
}
