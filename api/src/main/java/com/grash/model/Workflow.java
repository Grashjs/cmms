package com.grash.model;

import com.grash.model.abstracts.CompanyAudit;
import com.grash.model.enums.workflow.WFMainCondition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Workflow extends CompanyAudit {
    @NotNull
    private String title;
    @NotNull
    private WFMainCondition mainCondition;
    @OneToMany(cascade = CascadeType.ALL)
    private Collection<WorkflowCondition> secondaryConditions = new ArrayList<>();
    @OneToOne(cascade = CascadeType.ALL)
    @NotNull
    private WorkflowAction action;

    private boolean enabled = true;
}
