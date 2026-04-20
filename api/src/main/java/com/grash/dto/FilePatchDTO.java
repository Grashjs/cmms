package com.grash.dto;

import com.grash.model.enums.FileType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
@Schema(description = "DTO for patching an existing file")
public class FilePatchDTO {
    @Schema(description = "Name")
    @NotNull
    private String name;
}

