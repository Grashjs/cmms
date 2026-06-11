package com.grash.mapper;

import com.grash.dto.shiftConfiguration.ShiftConfigurationPatchDTO;
import com.grash.dto.shiftConfiguration.ShiftConfigurationPostDTO;
import com.grash.dto.shiftConfiguration.ShiftConfigurationShowDTO;
import com.grash.model.ShiftConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ShiftConfigurationMapper {

    ShiftConfiguration updateShiftConfiguration(@MappingTarget ShiftConfiguration entity,
                                                ShiftConfigurationPatchDTO dto);

    ShiftConfiguration fromPostDto(ShiftConfigurationPostDTO dto);

    ShiftConfigurationShowDTO toShowDto(ShiftConfiguration model);

}
