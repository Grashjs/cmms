package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class TeamMiniDTO {
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    Long id;
    
    @Schema(description = "Name")
    String name;
    
    @Schema(description = "List of team members")
    List<UserMiniDTO> users;
}
