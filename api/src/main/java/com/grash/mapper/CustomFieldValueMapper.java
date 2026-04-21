package com.grash.mapper;

import com.grash.dto.cutomField.CustomFieldValueShowDTO;
import com.grash.model.CustomFieldValue;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {CustomFieldMapper.class})
public interface CustomFieldValueMapper {

    CustomFieldValueShowDTO toShowDto(CustomFieldValue model);
}

