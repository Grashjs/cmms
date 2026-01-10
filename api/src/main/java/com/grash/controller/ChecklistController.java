package com.grash.controller;

import com.grash.dto.ChecklistPatchDTO;
import com.grash.dto.ChecklistPostDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.Checklist;
import com.grash.model.CompanySettings;
import com.grash.model.OwnUser;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PlanFeatures;
import com.grash.model.enums.RoleType;
import com.grash.service.ChecklistService;
import com.grash.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.Optional;

@RestController
@RequestMapping("/checklists")
@Tag(name = "checklist")
@RequiredArgsConstructor
public class ChecklistController {

    private final ChecklistService checklistService;
    private final UserService userService;

    @GetMapping("")
    @PreAuthorize("permitAll()")

    public Collection<Checklist> getAll(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            CompanySettings companySettings = user.getCompany().getCompanySettings();
            return checklistService.findByCompanySettings(companySettings.getId());
        } else return checklistService.getAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")

    public Checklist getById(@PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Checklist> optionalChecklist = checklistService.findById(id);
        if (optionalChecklist.isPresent()) {
            Checklist savedChecklist = optionalChecklist.get();
            return savedChecklist;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    Checklist create(@Valid @RequestBody ChecklistPostDTO checklistReq, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS)
                && user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.CHECKLIST)) {
            return checklistService.createPost(checklistReq, user.getCompany());
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")

    public Checklist patch(@Valid @RequestBody ChecklistPatchDTO checklist,
                           @PathVariable("id") Long id,
                           HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Checklist> optionalChecklist = checklistService.findById(id);

        if (optionalChecklist.isPresent()) {
            Checklist savedChecklist = optionalChecklist.get();
            if (user.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS)) {
                return checklistService.update(id, checklist, user.getCompany());
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Checklist not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")

    public ResponseEntity<SuccessResponse> delete(@PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<Checklist> optionalChecklist = checklistService.findById(id);
        if (optionalChecklist.isPresent()) {
            Checklist savedChecklist = optionalChecklist.get();
            if (user.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS)) {
                checklistService.delete(id);
                return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"),
                        HttpStatus.OK);
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Checklist not found", HttpStatus.NOT_FOUND);
    }

}


