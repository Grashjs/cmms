package com.grash.dto;

import com.grash.model.enums.CustomFieldEntityType;
import com.grash.model.enums.CustomFieldType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO for displaying a company custom field")
public class CustomFieldShowDTO extends AuditShowDTO {
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Field label")
    private String label;

    @Schema(description = "Field type")
    private CustomFieldType fieldType;

    @Schema(description = "Entity type this field applies to")
    private CustomFieldEntityType entityType;

    @Schema(description = "Whether this field is required")
    private boolean required;

    @Schema(description = "Whether to copy this field value when repeating work orders")
    private boolean copyOnRepeat;

    @Schema(description = "Options for single choice fields")
    private List<String> options;

    private int order;
}

