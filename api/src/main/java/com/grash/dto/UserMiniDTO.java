package com.grash.dto;

import com.grash.model.File;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserMiniDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private FileMiniDTO image;
    private String phone;
}
