package com.grash.service;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.advancedsearch.SpecificationBuilder;
import com.grash.dto.TeamPatchDTO;
import com.grash.dto.TeamShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.TeamMapper;
import com.grash.model.Notification;
import com.grash.model.OwnUser;
import com.grash.model.Team;
import com.grash.model.enums.NotificationType;
import com.grash.model.enums.RoleType;
import com.grash.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamService {
    private final TeamRepository teamRepository;
    private final CompanyService companyService;
    private final TeamMapper teamMapper;
    private final NotificationService notificationService;
    private final EntityManager em;
    private final MessageSource messageSource;
    private AssetService assetService;
    private LocationService locationService;
    private final UserService userService;

    @Autowired
    public void setDeps(@Lazy AssetService assetService, @Lazy LocationService locationService
    ) {
        this.assetService = assetService;
        this.locationService = locationService;
    }

    @Transactional
    public Team create(Team team) {
        Team savedTeam = teamRepository.saveAndFlush(team);
        em.refresh(savedTeam);
        return savedTeam;
    }

    @Transactional
    public Team update(Long id, TeamPatchDTO team) {
        if (teamRepository.existsById(id)) {
            Team savedTeam = teamRepository.findById(id).get();
            Team updatedTeam = teamRepository.saveAndFlush(teamMapper.updateTeam(savedTeam, team));
            em.refresh(updatedTeam);
            return updatedTeam;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public Collection<Team> getAll() {
        return teamRepository.findAll();
    }

    public void delete(Long id) {
        teamRepository.deleteById(id);
    }

    public Optional<Team> findById(Long id) {
        return teamRepository.findById(id);
    }

    public Collection<Team> findByCompany(Long id) {
        return teamRepository.findByCompany_Id(id);
    }

    public void notify(Team team, Locale locale) {
        String title = messageSource.getMessage("new_team", null, locale);
        String message = messageSource.getMessage("notification_team_added", new Object[]{team.getName()}, locale);
        if (team.getUsers() != null) {
            notificationService.createMultiple(team.getUsers().stream().map(assignedUser ->
                    new Notification(message, assignedUser, NotificationType.TEAM, team.getId())).collect(Collectors.toList()), true, title);
        }
    }

    public void patchNotify(Team oldTeam, Team newTeam, Locale locale) {
        String title = messageSource.getMessage("new_team", null, locale);
        String message = messageSource.getMessage("notification_team_added", new Object[]{newTeam.getName()}, locale);
        if (newTeam.getUsers() != null) {
            List<OwnUser> newUsers = newTeam.getUsers().stream().filter(
                    user -> oldTeam.getUsers().stream().noneMatch(user1 -> user1.getId().equals(user.getId()))).collect(Collectors.toList());
            notificationService.createMultiple(newUsers.stream().map(newUser ->
                    new Notification(message, newUser, NotificationType.TEAM, newTeam.getId())).collect(Collectors.toList()), true, title);
        }
    }

    public boolean isTeamInCompany(Team team, long companyId, boolean optional) {
        if (optional) {
            Optional<Team> optionalTeam = team == null ? Optional.empty() : findById(team.getId());
            return team == null || (optionalTeam.isPresent() && optionalTeam.get().getCompany().getId().equals(companyId));
        } else {
            Optional<Team> optionalTeam = findById(team.getId());
            return optionalTeam.isPresent() && optionalTeam.get().getCompany().getId().equals(companyId);
        }
    }

    public Collection<Team> findByUser(Long id) {
        return teamRepository.findByUsers_Id(id);
    }


    public Page<TeamShowDTO> findBySearchCriteria(SearchCriteria searchCriteria) {
        SpecificationBuilder<Team> builder = new SpecificationBuilder<>();
        searchCriteria.getFilterFields().forEach(builder::with);
        Pageable page = PageRequest.of(searchCriteria.getPageNum(), searchCriteria.getPageSize(),
                searchCriteria.getDirection(), searchCriteria.getSortField());
        return teamRepository.findAll(builder.build(), page).map(teamMapper::toShowDto);
    }

    public Optional<Team> findByNameIgnoreCaseAndCompany(String teamName, Long id) {
        return teamRepository.findByNameIgnoreCaseAndCompany_Id(teamName, id);
    }
}
