package com.grash.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@NoArgsConstructor
public class TeamShowDTO {
    Long id;

    String name;

    String description;

    Collection<UserMiniDTO> users;
}
