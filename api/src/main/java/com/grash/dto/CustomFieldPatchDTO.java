package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "DTO for patching an existing custom field")
public class CustomFieldPatchDTO {

    @Schema(description = "Name")
    private String name;

    @Schema(description = "Value")
    private String value;
}
