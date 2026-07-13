package com.grash.mapper;

import com.grash.dto.PartMiniDTO;
import com.grash.dto.PartPatchDTO;
import com.grash.dto.PartShowDTO;
import com.grash.dto.FileShowDTO;
import com.grash.model.Part;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = {CustomerMapper.class, VendorMapper.class, UserMapper.class, TeamMapper.class, FileMapper.class})
public interface PartMapper {
    Part updatePart(@MappingTarget Part entity, PartPatchDTO dto);

    @Mappings({})
    PartPatchDTO toPatchDto(Part model);

    @Mappings({})
    PartMiniDTO toMiniDto(Part model);

    PartShowDTO toShowDto(Part model);
}
