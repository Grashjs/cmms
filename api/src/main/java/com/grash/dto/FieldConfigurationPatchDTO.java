package com.grash.dto;

import com.grash.model.enums.FieldType;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FieldConfigurationPatchDTO {
    private FieldType fieldType;

}
