package com.grash.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.grash.model.abstracts.CompanyAudit;
import com.grash.model.enums.TaskType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@Data
@AllArgsConstructor
@Builder
@Schema(description = "Task base entity defining task templates")
public class TaskBase extends CompanyAudit {
    @NotNull
    @Schema(description = "Task label", requiredMode = Schema.RequiredMode.REQUIRED)
    private String label;

    @Schema(description = "Type of task")
    private TaskType taskType = TaskType.SUBTASK;

    @OneToMany(
            mappedBy = "taskBase",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JsonManagedReference
    private List<TaskOption> options = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Asset asset;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Meter meter;
}


