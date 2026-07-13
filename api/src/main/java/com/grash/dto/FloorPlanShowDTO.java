package com.grash.dto;

import lombok.Data;

@Data
public class FloorPlanShowDTO {
    private Long id;
    private String name;

    private FileShowDTO image;

    private long area;

}
