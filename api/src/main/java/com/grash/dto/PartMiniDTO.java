package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
public class PartMiniDTO {
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;
    
    @Schema(description = "Name")
    private String name;
    
    @Schema(description = "Description")
    private String description;
    
    @Schema(description = "Cost")
    private double cost;
    
    @Schema(description = "Unit of measurement")
    private String unit;
    
    @Schema(description = "Indicates whether this is a non-stock part")
    private boolean nonStock;
}
