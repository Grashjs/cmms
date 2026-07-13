package com.grash.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
public class PartMiniDTO {
    private Long id;
    private String name;
    private String description;
    private double cost;
    private String unit;
}
