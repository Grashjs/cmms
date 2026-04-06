package com.grash.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.model.File;
import com.grash.model.PreventiveMaintenance;
import com.grash.model.WorkOrder;
import com.grash.model.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "DTO for displaying task details in API responses")
public class TaskShowDTO extends AuditShowDTO {
    @Schema(description = "Base task information")
    private TaskBaseShowDTO taskBase;

    @Schema(description = "Task notes")
    private String notes;

    @Schema(description = "Task value")
    private String value;

    @Schema(description = "List of attached images")
    private List<FileShowDTO> images = new ArrayList<>();
}


