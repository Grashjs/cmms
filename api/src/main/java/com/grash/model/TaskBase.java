package com.grash.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.grash.model.abstracts.CompanyAudit;
import com.grash.model.enums.TaskType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;

@Entity
@NoArgsConstructor
@Data
@AllArgsConstructor
@Builder
public class TaskBase extends CompanyAudit {
    @NotNull
    private String label;

    private TaskType taskType = TaskType.SUBTASK;

    @OneToMany(
            mappedBy = "taskBase",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JsonManagedReference
    private Collection<TaskOption> options = new ArrayList<>();

    @ManyToOne
    private OwnUser user;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Asset asset;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Meter meter;
}
