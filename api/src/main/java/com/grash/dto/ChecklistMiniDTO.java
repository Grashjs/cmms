package com.grash.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
public class ChecklistMiniDTO {
    private Long id;
    private String name;
}
