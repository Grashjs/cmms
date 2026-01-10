package com.grash.mapper;

import com.grash.dto.WorkflowPatchDTO;
import com.grash.dto.WorkflowShowDTO;
import com.grash.model.Workflow;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = {WorkflowConditionMapper.class, WorkflowActionMapper.class})
public interface WorkflowMapper {
    Workflow updateWorkflow(@MappingTarget Workflow entity, WorkflowPatchDTO dto);

    @Mappings({})
    WorkflowPatchDTO toPatchDto(Workflow model);

    WorkflowShowDTO toShowDto(Workflow model);
}
