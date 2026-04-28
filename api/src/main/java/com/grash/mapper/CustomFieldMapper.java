package com.grash.mapper;

import com.grash.dto.cutomField.CustomFieldPatchDTO;
import com.grash.dto.cutomField.CustomFieldPostDTO;
import com.grash.dto.cutomField.CustomFieldShowDTO;
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

