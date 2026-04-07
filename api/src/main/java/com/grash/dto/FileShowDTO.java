package com.grash.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.grash.model.*;
import com.grash.model.enums.FileType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "DTO for displaying file details in API responses")
public class FileShowDTO extends AuditShowDTO {
    @Schema(description = "Name")
    private String name;

    @Schema(description = "File URL")
    private String url;

    @Schema(description = "File type")
    private FileType type = FileType.OTHER;

    @Schema(description = "Indicates whether the file is hidden")
    private boolean hidden = false;

}


