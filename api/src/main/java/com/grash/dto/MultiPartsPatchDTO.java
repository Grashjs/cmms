package com.grash.dto;

import com.grash.model.Part;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@NoArgsConstructor
public class MultiPartsPatchDTO {

    private String name;

    private Collection<Part> parts;
}
