package com.grash.mapper;

import com.grash.dto.WorkOrderMeterTriggerPatchDTO;
import com.grash.dto.WorkOrderMeterTriggerShowDTO;
import com.grash.model.WorkOrderMeterTrigger;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = {PartMapper.class, UserMapper.class, FileMapper.class})
public interface WorkOrderMeterTriggerMapper {
    WorkOrderMeterTrigger updateWorkOrderMeterTrigger(@MappingTarget WorkOrderMeterTrigger entity, WorkOrderMeterTriggerPatchDTO dto);

    @Mappings({})
    WorkOrderMeterTriggerPatchDTO toPatchDto(WorkOrderMeterTrigger model);

    @Mappings({})
    WorkOrderMeterTriggerShowDTO toShowDto(WorkOrderMeterTrigger model);
}
