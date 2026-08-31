package com.grash.mapper;

import com.grash.dto.TeamMiniDTO;
import com.grash.dto.TeamPatchDTO;
import com.grash.dto.TeamShowDTO;
import com.grash.model.Team;
import com.grash.model.User;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface TeamMapper {
    Team updateTeam(@MappingTarget Team entity, TeamPatchDTO dto);

    @Mappings({})
    TeamPatchDTO toPatchDto(Team model);

    TeamMiniDTO toMiniDto(Team model);

    TeamShowDTO toShowDto(Team model);

    @AfterMapping
    default void toMiniDto(Team model, @MappingTarget TeamMiniDTO target) {
        if (model.getUsers() != null) {
            List<Long> userIds = model.getUsers().stream()
                    .map(User::getId)
                    .collect(Collectors.toList());
            target.setUserIds(userIds);
        } else {
            target.setUserIds(Collections.emptyList());
        }
    }
}
