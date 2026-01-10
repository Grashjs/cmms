package com.grash.mapper;

import com.grash.dto.TaskBasePatchDTO;
import com.grash.dto.TaskBaseShowDTO;
import com.grash.model.TaskBase;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = {TaskOptionMapper.class})
public interface TaskBaseMapper {
    TaskBase updateTaskBase(@MappingTarget TaskBase entity, TaskBasePatchDTO dto);

    @Mappings({})
    TaskBasePatchDTO toPatchDto(TaskBase model);

    TaskBaseShowDTO toShowDto(TaskBase model);
}
