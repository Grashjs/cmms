package com.grash.mapper;

import com.grash.dto.FloorPlanPatchDTO;
import com.grash.dto.FloorPlanShowDTO;
import com.grash.model.FloorPlan;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface FloorPlanMapper {
    FloorPlan updateFloorPlan(@MappingTarget FloorPlan entity, FloorPlanPatchDTO dto);

    @Mappings({})
    FloorPlanPatchDTO toPatchDto(FloorPlan model);

    FloorPlanShowDTO toShowDto(FloorPlan model);
}
