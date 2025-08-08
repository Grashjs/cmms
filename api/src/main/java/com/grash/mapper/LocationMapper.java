package com.grash.mapper;

import com.grash.dto.AssetShowDTO;
import com.grash.dto.FileShowDTO;
import com.grash.dto.LocationMiniDTO;
import com.grash.dto.LocationPatchDTO;
import com.grash.dto.LocationShowDTO;
import com.grash.model.Asset;
import com.grash.model.Location;
import com.grash.service.AssetService;
import com.grash.service.LocationService;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {CustomerMapper.class, VendorMapper.class, UserMapper.class,
        TeamMapper.class, FileMapper.class})
public interface LocationMapper {
    Location updateLocation(@MappingTarget Location entity, LocationPatchDTO dto);

    @Mappings({})
    LocationPatchDTO toPatchDto(Location model);

    LocationShowDTO toShowDto(Location model, @Context LocationService locationService);

    @Mapping(source = "parentLocation.id", target = "parentId")
    LocationMiniDTO toMiniDto(Location model);

    @AfterMapping
    default LocationShowDTO toShowDto(Location model, @MappingTarget LocationShowDTO target,
                                      @Context LocationService locationService) {
        target.setHasChildren(locationService.hasChildren(model.getId()));
        return target;
    }
}
