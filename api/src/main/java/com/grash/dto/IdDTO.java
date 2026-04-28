package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "DTO containing a unique identifier reference")
public class IdDTO {
    @Schema(description = "Unique identifier")
    private Long id;
}
