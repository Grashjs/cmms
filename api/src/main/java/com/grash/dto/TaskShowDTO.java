package com.grash.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.model.File;
import com.grash.model.PreventiveMaintenance;
import com.grash.model.WorkOrder;
import com.grash.model.enums.Status;
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
public class TaskShowDTO extends AuditShowDTO {
    private TaskBaseShowDTO taskBase;

    private String notes;

    private String value;

    private List<FileShowDTO> images = new ArrayList<>();
}


