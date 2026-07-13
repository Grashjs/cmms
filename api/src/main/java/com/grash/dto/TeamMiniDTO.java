package com.grash.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class TeamMiniDTO {
    Long id;
    String name;
    List<UserMiniDTO> users;
}
