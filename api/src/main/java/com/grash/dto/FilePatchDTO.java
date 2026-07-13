package com.grash.dto;

import com.grash.model.enums.FileType;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class FilePatchDTO {
    @NotNull
    private String name;
}
