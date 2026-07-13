package com.grash.mapper;

import com.grash.dto.CategoryPatchDTO;
import com.grash.model.PurchaseOrderCategory;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface PurchaseOrderCategoryMapper {
    PurchaseOrderCategory updatePurchaseOrderCategory(@MappingTarget PurchaseOrderCategory entity, CategoryPatchDTO dto);

    @Mappings({})
    CategoryPatchDTO toPatchDto(PurchaseOrderCategory model);
}
