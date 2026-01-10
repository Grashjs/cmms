package com.grash.dto;

import com.grash.model.OwnUser;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@NoArgsConstructor
public class TeamPatchDTO {
    String name;
    String description;

    @ArraySchema(schema = @Schema(implementation = IdDTO.class))
    Collection<OwnUser> users;
}
