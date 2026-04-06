package com.grash.dto;

import com.grash.model.enums.FieldType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "DTO for patching field configuration")
public class FieldConfigurationPatchDTO {
    @Schema(description = "Field type")
    private FieldType fieldType;

}
