package com.grash.dto;

import com.grash.model.OwnUser;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@NoArgsConstructor
public class TeamPatchDTO {
    String name;
    String description;

    Collection<OwnUser> users;
}
