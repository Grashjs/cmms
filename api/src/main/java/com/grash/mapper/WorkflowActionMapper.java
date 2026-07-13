package com.grash.mapper;

import com.grash.dto.WorkflowActionPatchDTO;
import com.grash.dto.WorkflowActionPostDTO;
import com.grash.model.WorkflowAction;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface WorkflowActionMapper {
    WorkflowAction updateWorkflowAction(@MappingTarget WorkflowAction entity, WorkflowActionPatchDTO dto);

    @Mappings({})
    WorkflowActionPatchDTO toPatchDto(WorkflowAction model);

    @Mappings({})
    WorkflowAction toModel(WorkflowActionPostDTO dto);
}
