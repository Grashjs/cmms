package com.grash.mapper;

import com.grash.dto.CustomFieldPatchDTO;
import com.grash.dto.CustomFieldPostDTO;
import com.grash.dto.CustomFieldShowDTO;
import com.grash.model.CustomField;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface CustomFieldMapper {
    CustomField updateCustomField(@MappingTarget CustomField entity, CustomFieldPatchDTO dto);

    @Mappings({})
    CustomFieldPatchDTO toPatchDto(CustomField model);

    @Mappings({})
    CustomField toModel(CustomFieldPostDTO model);

    @Mappings({})
    CustomFieldShowDTO toShowDto(CustomField model);
}

