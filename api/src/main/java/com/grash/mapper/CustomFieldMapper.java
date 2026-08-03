package com.grash.mapper;

import com.grash.dto.cutomField.CustomFieldPatchDTO;
import com.grash.dto.cutomField.CustomFieldPostDTO;
import com.grash.dto.cutomField.CustomFieldShowDTO;
import com.grash.model.CustomField;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface CustomFieldMapper {
    // assetCategories is resolved from assetCategoryIds in CustomFieldService, which has to
    // look the categories up and verify they belong to the caller's company. Letting
    // MapStruct near it would produce detached entities or silently drop the binding, so it
    // is ignored here on purpose.
    @Mapping(target = "assetCategories", ignore = true)
    CustomField updateCustomField(@MappingTarget CustomField entity, CustomFieldPatchDTO dto);

    @Mappings({})
    CustomFieldPatchDTO toPatchDto(CustomField model);

    @Mapping(target = "assetCategories", ignore = true)
    CustomField toModel(CustomFieldPostDTO model);

    @Mappings({})
    CustomFieldShowDTO toShowDto(CustomField model);
}
