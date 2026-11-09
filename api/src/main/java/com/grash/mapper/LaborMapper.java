package com.grash.mapper;

import com.grash.dto.LaborPatchDTO;
import com.grash.model.Labor;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LaborMapper {
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "timerRunId", ignore = true)
    Labor updateLabor(@MappingTarget Labor entity, LaborPatchDTO dto);

    @Mappings({})
    LaborPatchDTO toPatchDto(Labor model);
}
