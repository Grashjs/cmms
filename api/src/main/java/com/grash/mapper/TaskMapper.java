package com.grash.mapper;

import com.grash.dto.TaskPatchDTO;
import com.grash.dto.TaskShowDTO;
import com.grash.model.Task;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {FileMapper.class, TaskBaseMapper.class})
public interface TaskMapper {
    @Mapping(target = "version", ignore = true)
    default Task updateTask(@MappingTarget Task entity, TaskPatchDTO dto) {
        if (dto.hasValue()) entity.setValue(dto.getValue());
        if (dto.hasNotes()) entity.setNotes(dto.getNotes());
        return entity;
    }

    @Mappings({})
    TaskPatchDTO toPatchDto(Task model);

    TaskShowDTO toShowDto(Task model);
}
