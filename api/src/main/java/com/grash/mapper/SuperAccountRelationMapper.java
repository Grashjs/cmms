package com.grash.mapper;

import com.grash.dto.RolePatchDTO;
import com.grash.dto.SuperAccountRelationDTO;
import com.grash.model.Role;
import com.grash.model.SuperAccountRelation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface SuperAccountRelationMapper {

    @Mappings({
            @Mapping(source = "childUser.company.name", target = "childCompanyName"),
            @Mapping(source = "childUser.id", target = "childUserId"),
            @Mapping(source = "superUser.id", target = "superUserId"),
    })
    SuperAccountRelationDTO toDto(SuperAccountRelation model);
}
