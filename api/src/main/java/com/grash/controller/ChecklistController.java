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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Collection;
import java.util.Optional;

@RestController
@RequestMapping("/checklists")
@Api(tags = "checklist")
@RequiredArgsConstructor
public class ChecklistController {

    private final ChecklistService checklistService;
    private final UserService userService;

    @GetMapping("")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "AssetCategory not found")})
    public Collection<Checklist> getAll(HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            CompanySettings companySettings = user.getCompany().getCompanySettings();
            return checklistService.findByCompanySettings(companySettings.getId());
        } else return checklistService.getAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Checklist not found")})
    public Checklist getById(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Checklist> optionalChecklist = checklistService.findById(id);
        if (optionalChecklist.isPresent()) {
            Checklist savedChecklist = optionalChecklist.get();
            return savedChecklist;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied")})
    public Checklist create(@ApiParam("Checklist") @Valid @RequestBody ChecklistPostDTO checklistReq, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        if (user.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS)
                && user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.CHECKLIST)) {
            return checklistService.createPost(checklistReq, user.getCompany());
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "Checklist not found")})
    public Checklist patch(@ApiParam("Checklist") @Valid @RequestBody ChecklistPatchDTO checklist, @ApiParam("id") @PathVariable("id") Long id,
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
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "Checklist not found")})
    public ResponseEntity<SuccessResponse> delete(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
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
