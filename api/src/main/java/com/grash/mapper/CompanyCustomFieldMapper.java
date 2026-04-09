package com.grash.mapper;

import com.grash.dto.CompanyCustomFieldPatchDTO;
import com.grash.dto.CompanyCustomFieldPostDTO;
import com.grash.dto.CompanyCustomFieldShowDTO;
import com.grash.model.CompanyCustomField;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface CompanyCustomFieldMapper {
    CompanyCustomField updateCompanyCustomField(@MappingTarget CompanyCustomField entity, CompanyCustomFieldPatchDTO dto);

    @Mappings({})
    CompanyCustomFieldPatchDTO toPatchDto(CompanyCustomField model);

    @Mappings({})
    CompanyCustomField toModel(CompanyCustomFieldPostDTO model);

    @Mappings({})
    CompanyCustomFieldShowDTO toShowDto(CompanyCustomField model);
}

