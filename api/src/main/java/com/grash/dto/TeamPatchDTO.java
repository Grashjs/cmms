package com.grash.dto;

import com.grash.model.OwnUser;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@NoArgsConstructor
@Schema(description = "DTO for patching an existing team")
public class TeamPatchDTO {
    @Schema(description = "Name")
    String name;
    
    @Schema(description = "Description")
    String description;

    @ArraySchema(schema = @Schema(implementation = IdDTO.class))
    @Schema(description = "List of team members")
    Collection<OwnUser> users;
}
