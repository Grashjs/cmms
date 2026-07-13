package com.grash.mapper;

import com.grash.dto.WorkflowPatchDTO;
import com.grash.model.Workflow;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface WorkflowMapper {
    Workflow updateWorkflow(@MappingTarget Workflow entity, WorkflowPatchDTO dto);

    @Mappings({})
    WorkflowPatchDTO toPatchDto(Workflow model);
}
