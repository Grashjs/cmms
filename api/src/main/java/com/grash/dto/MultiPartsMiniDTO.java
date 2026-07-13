package com.grash.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class MultiPartsMiniDTO {
    private Long id;
    private String name;
    private List<PartMiniDTO> parts;
}
