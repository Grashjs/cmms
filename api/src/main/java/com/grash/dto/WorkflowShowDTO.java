package com.grash.dto;

import com.grash.model.enums.workflow.WFMainCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;

@Data
@NoArgsConstructor
@Schema(description = "Workflow configuration showing conditions, actions, and enablement status")
public class WorkflowShowDTO extends AuditShowDTO {
    @Schema(description = "Title of the workflow rule")
    private String title;
    @Schema(description = "Main condition that triggers the workflow")
    private WFMainCondition mainCondition;
    @Schema(description = "Secondary conditions for additional filtering")
    private Collection<WorkflowConditionShowDTO> secondaryConditions = new ArrayList<>();
    @Schema(description = "Action to execute when conditions are met")
    private WorkflowActionShowDTO action;
    @Schema(description = "Whether the workflow is currently active")
    private boolean enabled;
}
