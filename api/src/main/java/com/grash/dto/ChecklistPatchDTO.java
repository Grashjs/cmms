package com.grash.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.List;

@Data
@NoArgsConstructor
public class ChecklistPatchDTO {
    private String name;

    private String description;

    private List<TaskBaseDTO> taskBases;

    private String category;

}
