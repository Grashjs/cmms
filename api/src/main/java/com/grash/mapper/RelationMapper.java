package com.grash.mapper;

import com.grash.dto.RelationPatchDTO;
import com.grash.dto.RelationPostDTO;
import com.grash.model.Relation;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface RelationMapper {
    Relation updateRelation(@MappingTarget Relation entity, RelationPatchDTO dto);

    @Mappings({})
    RelationPatchDTO toPatchDto(RelationPostDTO model);
}
