package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.SuccessResponse;
import com.grash.dto.TeamMiniDTO;
import com.grash.dto.TeamPatchDTO;
import com.grash.dto.TeamShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.TeamMapper;
import com.grash.model.OwnUser;
import com.grash.model.Team;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.service.TeamService;
import com.grash.service.UserService;
import com.grash.utils.Helper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/teams")
@Api(tags = "team")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final TeamMapper teamMapper;
    private final UserService userService;
    private final EntityManager em;

    @PostMapping("/search")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<TeamShowDTO>> search(@RequestBody SearchCriteria searchCriteria, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            if (user.getRole().getViewPermissions().contains(PermissionEntity.PEOPLE_AND_TEAMS)) {
                searchCriteria.filterCompany(user);
            } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(teamService.findBySearchCriteria(searchCriteria));
    }

    @GetMapping("/mini")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "AssetCategory not found")})
    public Collection<TeamMiniDTO> getMini(HttpServletRequest req) {
        OwnUser team = userService.whoami(req);
        return teamService.findByCompany(team.getCompany().getId()).stream().map(teamMapper::toMiniDto).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Team not found")})
    public TeamShowDTO getById(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Team> optionalTeam = teamService.findById(id);
        if (optionalTeam.isPresent()) {
            Team savedTeam = optionalTeam.get();
            return teamMapper.toShowDto(savedTeam);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied")})
    public TeamShowDTO create(@ApiParam("Team") @Valid @RequestBody Team teamReq, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.PEOPLE_AND_TEAMS)) {
            Team savedTeam = teamService.create(teamReq);
            teamService.notify(savedTeam, Helper.getLocale(user));
            return teamMapper.toShowDto(savedTeam);
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "Team not found")})
    public TeamShowDTO patch(@ApiParam("Team") @Valid @RequestBody TeamPatchDTO team, @ApiParam("id") @PathVariable("id") Long id,
                             HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Team> optionalTeam = teamService.findById(id);
        if (optionalTeam.isPresent()) {
            Team savedTeam = optionalTeam.get();
            em.detach(savedTeam);
            Team patchTeam = teamService.update(id, team);
            teamService.patchNotify(savedTeam, patchTeam, Helper.getLocale(user));
            return teamMapper.toShowDto(patchTeam);
        } else throw new CustomException("Team not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "Team not found")})
    public ResponseEntity delete(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<Team> optionalTeam = teamService.findById(id);
        if (optionalTeam.isPresent()) {
            Team savedTeam = optionalTeam.get();
            if (savedTeam.getCreatedBy().equals(user.getId()) || user.getRole().getDeleteOtherPermissions().contains(PermissionEntity.PEOPLE_AND_TEAMS)) {
                teamService.delete(id);
                return new ResponseEntity(new SuccessResponse(true, "Deleted successfully"),
                        HttpStatus.OK);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Team not found", HttpStatus.NOT_FOUND);
    }

}
