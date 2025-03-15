package com.grash.mapper;

import com.grash.dto.UiConfigurationPatchDTO;
import com.grash.model.UiConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface UiConfigurationMapper {
    UiConfiguration updateUiConfiguration(@MappingTarget UiConfiguration entity, UiConfigurationPatchDTO dto);
}
