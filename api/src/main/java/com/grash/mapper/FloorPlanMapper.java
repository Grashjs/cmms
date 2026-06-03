package com.grash.mapper;

import com.grash.dto.FloorPlanPatchDTO;
import com.grash.dto.FloorPlanShowDTO;
import com.grash.mapper.FileMapper;
import com.grash.model.FloorPlan;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = {FileMapper.class})
public interface FloorPlanMapper {
    FloorPlan updateFloorPlan(@MappingTarget FloorPlan entity, FloorPlanPatchDTO dto);

    @Mappings({})
    FloorPlanPatchDTO toPatchDto(FloorPlan model);

    @Mapping(target = "image", source = "image", qualifiedByName = "toThumbnailDto")
    FloorPlanShowDTO toShowDto(FloorPlan model);
}
