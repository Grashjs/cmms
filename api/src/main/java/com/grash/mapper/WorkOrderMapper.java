package com.grash.mapper;

import com.grash.dto.WorkOrderBaseMiniDTO;
import com.grash.dto.WorkOrderPatchDTO;
import com.grash.dto.WorkOrderShowDTO;
import com.grash.dto.workOrder.WorkOrderPostDTO;
import com.grash.model.WorkOrder;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {PartMapper.class, FileMapper.class, LocationMapper.class
        , TeamMapper.class, UserMapper.class,
        CustomerMapper.class, AssetMapper.class})
public interface WorkOrderMapper {
    WorkOrder updateWorkOrder(@MappingTarget WorkOrder entity, WorkOrderPatchDTO dto);

    @Mappings({})
    WorkOrderPatchDTO toPatchDto(WorkOrder model);

    @Mappings({
            @Mapping(source = "parentRequest.audioDescription", target = "audioDescription")
    })
    WorkOrderShowDTO toShowDto(WorkOrder model);

    WorkOrderBaseMiniDTO toBaseMiniDto(WorkOrder model);

    WorkOrder fromPostDto(WorkOrderPostDTO workOrderPostDTO);

}
