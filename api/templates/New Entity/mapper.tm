package com.grash.mapper;

import com.grash.dto.{name}[-c].{name}[-C]PatchDTO;
import com.grash.dto.{name}[-c].{name}[-C]PostDTO;
import com.grash.dto.{name}[-c].{name}[-C]ShowDTO;
import com.grash.model.{name}[-C];
import jakarta.validation.Valid;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface {name}[-C]Mapper {
    {name}[-C] update{name}[-C](@MappingTarget {name}[-C] entity,
                                                        {name}[-C]PatchDTO dto);

    {name}[-C] fromPostDto(@Valid {name}[-C]PostDTO dto);

    {name}[-C]ShowDTO toShowDto(@Valid {name}[-C] model);
}
