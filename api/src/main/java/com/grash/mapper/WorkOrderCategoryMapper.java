package com.grash.mapper;

import com.grash.dto.CategoryPatchDTO;
import com.grash.model.WorkOrderCategory;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface WorkOrderCategoryMapper {
    WorkOrderCategory updateWorkOrderCategory(@MappingTarget WorkOrderCategory entity, CategoryPatchDTO dto);

    @Mappings({})
    CategoryPatchDTO toPatchDto(WorkOrderCategory model);
}
