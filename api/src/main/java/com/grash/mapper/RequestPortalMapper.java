package com.grash.mapper;

import com.grash.dto.requestPortal.RequestPortalPatchDTO;
import com.grash.dto.requestPortal.RequestPortalPostDTO;
import com.grash.dto.requestPortal.RequestPortalShowDTO;
import com.grash.model.RequestPortal;
import jakarta.validation.Valid;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface RequestPortalMapper {
    RequestPortal updateRequestPortal(@MappingTarget RequestPortal entity,
                                                        RequestPortalPatchDTO dto);

    RequestPortal fromPostDto(@Valid RequestPortalPostDTO dto);

    RequestPortalShowDTO toShowDto(@Valid RequestPortal model);
}
