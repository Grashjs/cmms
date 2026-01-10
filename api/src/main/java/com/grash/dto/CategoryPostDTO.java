package com.grash.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
public class CategoryPostDTO {

    @NotNull
    private String name;

    private String description;
}

