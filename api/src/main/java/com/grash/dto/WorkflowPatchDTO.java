package com.grash.dto;

import com.grash.model.WorkflowAction;
import com.grash.model.WorkflowCondition;
import com.grash.model.enums.workflow.WFMainCondition;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class WorkflowPatchDTO {
    private String title;
    private WFMainCondition mainCondition;
    @ArraySchema(schema = @Schema(implementation = IdDTO.class))
    private List<WorkflowCondition> secondaryConditions;
    @Schema(implementation = IdDTO.class)
    private WorkflowAction action;
}
