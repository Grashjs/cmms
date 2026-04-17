package com.grash.dto;

import com.grash.model.enums.CustomFieldEntityType;
import com.grash.model.enums.CustomFieldType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "DTO for creating a new company custom field")
public class CustomFieldPostDTO {
    @Schema(description = "Field label", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private String label;

    @Schema(description = "Field type", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private CustomFieldType fieldType;

    @Schema(description = "Entity type this field applies to")
    @NotNull
    private CustomFieldEntityType entityType;

    @Schema(description = "Whether this field is required")
    private boolean required = false;

    @Schema(description = "Whether to copy this field value when repeating work orders")
    private boolean copyOnRepeat = false;

    @Schema(description = "Options for single choice fields")
    private List<String> options;
}

