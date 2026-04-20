package com.grash.dto;

import com.grash.model.enums.workflow.WFMainCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Workflow configuration to create")
public class WorkflowPostDTO {
    @NotNull
    @Schema(description = "Workflow title")
    private String title;
    @NotNull
    @Schema(description = "Main condition that triggers the workflow")
    private WFMainCondition mainCondition;
    @Schema(description = "Secondary conditions for the workflow")
    private List<WorkflowConditionPostDTO> secondaryConditions = new ArrayList<>();
    @NotNull
    @Schema(description = "Action to execute when workflow triggers")
    private WorkflowActionPostDTO action;
}

