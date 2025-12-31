package com.grash.mapper;

import com.grash.dto.WorkOrderMessagePatchDTO;
import com.grash.dto.WorkOrderMessageShowDTO;
import com.grash.model.WorkOrderMessage;
import com.grash.service.WorkOrderMessageService;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {UserMapper.class, FileMapper.class})
public interface WorkOrderMessageMapper {

    WorkOrderMessage updateWorkOrderMessage(@MappingTarget WorkOrderMessage entity, WorkOrderMessagePatchDTO dto);

    @Mapping(target = "workOrderId", source = "workOrder.id")
    @Mapping(target = "parentMessageId", source = "parentMessage.id")
    @Mapping(target = "reactions", ignore = true)
    @Mapping(target = "readBy", ignore = true)
    @Mapping(target = "readByCurrentUser", ignore = true)
    WorkOrderMessageShowDTO toShowDto(WorkOrderMessage model);

    WorkOrderMessageShowDTO toShowDto(WorkOrderMessage model, @Context WorkOrderMessageService workOrderMessageService);
}
