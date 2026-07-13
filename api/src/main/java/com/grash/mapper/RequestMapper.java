package com.grash.mapper;

import com.grash.dto.RequestPatchDTO;
import com.grash.dto.RequestShowDTO;
import com.grash.model.Request;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = {FileMapper.class})
public interface RequestMapper {
    Request updateRequest(@MappingTarget Request entity, RequestPatchDTO dto);

    @Mappings({})
    RequestPatchDTO toPatchDto(Request model);

    RequestShowDTO toShowDto(Request model);
}
