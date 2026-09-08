package com.grash.service;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.advancedsearch.SpecificationBuilder;
import com.grash.dto.TeamPatchDTO;
import com.grash.dto.TeamShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.TeamMapper;
import com.grash.model.Notification;
import com.grash.model.User;
import com.grash.model.Team;
import com.grash.model.enums.NotificationType;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.repository.TeamRepository;
import com.grash.utils.Helper;
import com.grash.utils.Sanitizer;
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

import jakarta.persistence.EntityManager;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamService {
    private final TeamRepository teamRepository;
    private final TeamMapper teamMapper;
    private final NotificationService notificationService;
    private final EntityManager em;
    private final MessageSource messageSource;

    @Transactional
    public Team update(Long id, TeamPatchDTO team) {
        if (teamRepository.existsById(id)) {
            Team savedTeam = teamRepository.findById(id).get();
            Team updatedTeam = teamMapper.updateTeam(savedTeam, team);
            Sanitizer.sanitizeTeam(updatedTeam);
            updatedTeam = teamRepository.saveAndFlush(updatedTeam);
            em.refresh(updatedTeam);
            return updatedTeam;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public SearchCriteria getSearchCriteria(User user, SearchCriteria searchCriteria) {
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            if (user.getRole().getViewPermissions().contains(PermissionEntity.PEOPLE_AND_TEAMS)) {
                searchCriteria.filterCompany(user);
            } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
        return searchCriteria;
    }

    @Transactional
    public Team create(Team teamReq, User user) {
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.PEOPLE_AND_TEAMS)) {
            Sanitizer.sanitizeTeam(teamReq);
            Team savedTeam = teamRepository.saveAndFlush(teamReq);
            em.refresh(savedTeam);
            notify(savedTeam, Helper.getLocale(user));
            return savedTeam;
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    public Team getById(Long id, User user) {
        Optional<Team> optionalTeam = findById(id);
        if (optionalTeam.isPresent()) {
            Team savedTeam = optionalTeam.get();
            if (!savedTeam.canBeViewedBy(user))
                throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            return savedTeam;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @Transactional
    public Team patch(Long id, TeamPatchDTO team, User user) {
        Optional<Team> optionalTeam = findById(id);
        if (optionalTeam.isPresent()) {
            Team savedTeam = optionalTeam.get();
            if (!savedTeam.canBeEditedBy(user)) {
                throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            }
            em.detach(savedTeam);
            Team patchTeam = update(id, team);
            patchNotify(savedTeam, patchTeam, Helper.getLocale(user));
            return patchTeam;
        } else throw new CustomException("Team not found", HttpStatus.NOT_FOUND);
    }

    @Transactional
    public void deleteByIdAndUser(Long id, User user) {
        Optional<Team> optionalTeam = findById(id);
        if (optionalTeam.isPresent()) {
            Team savedTeam = optionalTeam.get();
            if (savedTeam.canBeDeletedBy(user)) {
                teamRepository.deleteById(id);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Team not found", HttpStatus.NOT_FOUND);
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
            List<User> newUsers = newTeam.getUsers().stream().filter(
                    user -> oldTeam.getUsers().stream().noneMatch(user1 -> user1.getId().equals(user.getId()))).collect(Collectors.toList());
            notificationService.createMultiple(newUsers.stream().map(newUser ->
                    new Notification(message, newUser, NotificationType.TEAM, newTeam.getId())).collect(Collectors.toList()), true, title);
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

