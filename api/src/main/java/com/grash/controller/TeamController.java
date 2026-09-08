package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.SuccessResponse;
import com.grash.dto.TeamMiniDTO;
import com.grash.dto.TeamPatchDTO;
import com.grash.dto.TeamShowDTO;
import com.grash.mapper.TeamMapper;
import com.grash.security.CurrentUser;
import com.grash.model.Team;
import com.grash.model.User;
import com.grash.service.TeamService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.Collection;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/teams")
@Tag(name = "Teams", description = "Operations on teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final TeamMapper teamMapper;

    @PostMapping("/search")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<TeamShowDTO>> search(@Parameter(description = "Search criteria for filtering teams") @RequestBody SearchCriteria searchCriteria,
                                                    @Parameter(hidden = true) @CurrentUser User user) {
        return ResponseEntity.ok(teamService.findBySearchCriteria(teamService.getSearchCriteria(user, searchCriteria)));
    }

    @GetMapping("/mini")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public Collection<TeamMiniDTO> getMini(@Parameter(hidden = true) @CurrentUser User user) {
        return teamService.findByCompany(user.getCompany().getId()).stream().map(teamMapper::toMiniDto).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public TeamShowDTO getById(@PathVariable("id") Long id, @Parameter(hidden = true) @CurrentUser User user) {
        return teamMapper.toShowDto(teamService.getById(id, user));
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    TeamShowDTO create(@Parameter(description = "Team data to create") @Valid @RequestBody Team teamReq,
                       @Parameter(hidden = true) @CurrentUser User user) {
        return teamMapper.toShowDto(teamService.create(teamReq, user));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public TeamShowDTO patch(@Parameter(description = "Team fields to update") @Valid @RequestBody TeamPatchDTO team,
                             @PathVariable Long id,
                             @Parameter(hidden = true) @CurrentUser User user) {
        return teamMapper.toShowDto(teamService.patch(id, team, user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> delete(@PathVariable Long id,
                                                  @Parameter(hidden = true) @CurrentUser User user) {
        teamService.deleteByIdAndUser(id, user);
        return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"),
                HttpStatus.OK);
    }

}