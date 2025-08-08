package com.grash.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LocationMiniDTO {

    private Long id;

    private String name;

    private String address;

    private String customId;

    private Long parentId;

}
