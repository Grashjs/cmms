package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@NoArgsConstructor
@Schema(description = "DTO for displaying team details in API responses")
public class TeamShowDTO {
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    Long id;

    @Schema(description = "Name")
    String name;

    @Schema(description = "Description")
    String description;

    @Schema(description = "List of team members")
    Collection<UserMiniDTO> users;
}
