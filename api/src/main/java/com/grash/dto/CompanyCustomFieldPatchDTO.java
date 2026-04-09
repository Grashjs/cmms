package com.grash.dto;

import com.grash.model.enums.CustomFieldEntityType;
import com.grash.model.enums.CustomFieldType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "DTO for patching an existing company custom field")
public class CompanyCustomFieldPatchDTO {
    @Schema(description = "Field label")
    private String label;

    @Schema(description = "Field type")
    private CustomFieldType fieldType;

    @Schema(description = "Whether this field is required")
    private Boolean required;

    @Schema(description = "Whether to copy this field value when repeating work orders")
    private Boolean copyOnRepeat;

    @Schema(description = "Options for single choice fields")
    private List<String> options;
}

