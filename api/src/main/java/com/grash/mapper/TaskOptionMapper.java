package com.grash.mapper;

import com.grash.dto.TaskOptionPatchDTO;
import com.grash.model.TaskOption;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface TaskOptionMapper {
    TaskOption updateTaskOption(@MappingTarget TaskOption entity, TaskOptionPatchDTO dto);

    @Mappings({})
    TaskOptionPatchDTO toPatchDto(TaskOption model);

}
