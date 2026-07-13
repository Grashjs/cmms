package com.grash.mapper;

import com.grash.dto.VendorMiniDTO;
import com.grash.dto.VendorPatchDTO;
import com.grash.model.Vendor;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface VendorMapper {
    Vendor updateVendor(@MappingTarget Vendor entity, VendorPatchDTO dto);

    @Mappings({})
    VendorPatchDTO toPatchDto(Vendor model);

    VendorMiniDTO toMiniDto(Vendor model);
}
