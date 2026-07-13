package com.grash.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FileMiniDTO {
    private Long id;
    private String name;
    private String url;
}
