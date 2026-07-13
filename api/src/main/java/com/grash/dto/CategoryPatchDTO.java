package com.grash.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
public class CategoryPatchDTO {
    @NotNull
    private String name;
    private String description;
}
