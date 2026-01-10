package com.grash.mapper;

import com.grash.dto.ChecklistMiniDTO;
import com.grash.model.Checklist;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChecklistMapper {
    ChecklistMiniDTO toMiniDto(Checklist model);
}
