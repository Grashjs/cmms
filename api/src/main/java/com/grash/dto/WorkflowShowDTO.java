package com.grash.dto;

import com.grash.model.enums.workflow.WFMainCondition;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;

@Data
@NoArgsConstructor
public class WorkflowShowDTO extends AuditShowDTO {
    private String title;
    private WFMainCondition mainCondition;
    private Collection<WorkflowConditionShowDTO> secondaryConditions = new ArrayList<>();
    private WorkflowActionShowDTO action;
    private boolean enabled;
}
