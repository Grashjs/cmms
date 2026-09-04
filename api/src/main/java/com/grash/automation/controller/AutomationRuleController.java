package com.grash.automation.controller;

import com.grash.automation.AutomationMetaService;
import com.grash.automation.AutomationRuleService;
import com.grash.automation.AutomationRunService;
import com.grash.automation.dto.AutomationMetaDTO;
import com.grash.automation.dto.AutomationRulePostDTO;
import com.grash.automation.dto.AutomationRuleShowDTO;
import com.grash.automation.dto.AutomationRunShowDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.User;
import com.grash.model.enums.PermissionEntity;
import com.grash.service.UserService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Rule administration. Deliberately a separate endpoint from {@code /workflows}: the old engine
 * keeps its API unchanged, so nothing that talks to it — including the untouched upstream mobile
 * app — has to know this exists.
 *
 * <p>Every read and write is scoped to the caller's own company. That is stated because the old
 * controller does not do it: {@code GET /workflows} returns the rules of <em>every</em> company
 * for a non-client role.
 */
@RestController
@RequestMapping("/automation-rules")
@Tag(name = "Automation", description = "Rule automation: triggers, conditions, actions and their run log")
@RequiredArgsConstructor
public class AutomationRuleController {

    private final AutomationRuleService ruleService;
    private final AutomationRunService runService;
    private final AutomationMetaService metaService;
    private final UserService userService;

    /**
     * What the rule editor is built from: the triggers, subjects, actions and placeholders this
     * server actually supports, including this company's custom fields.
     *
     * <p>The frontend holds no copy of any of it. That is the fix for the defect that made the
     * old settings form offer conditions nothing evaluated — four hand-kept lists that drifted
     * apart — and it is why this endpoint comes before the editor rather than after it.
     */
    @GetMapping("/meta")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public AutomationMetaDTO getMeta(HttpServletRequest request) {
        User user = requireSettingsView(request);
        return metaService.describe(user.getCompany());
    }

    @GetMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public Collection<AutomationRuleShowDTO> getAll(HttpServletRequest request) {
        User user = requireSettingsView(request);
        return ruleService.findByCompany(user.getCompany().getId()).stream()
                .map(AutomationRuleShowDTO::of)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public AutomationRuleShowDTO getById(@PathVariable("id") Long id, HttpServletRequest request) {
        User user = requireSettingsView(request);
        return ruleService.findByIdAndCompany(id, user.getCompany().getId())
                .map(AutomationRuleShowDTO::of)
                .orElseThrow(() -> new CustomException("Rule not found", HttpStatus.NOT_FOUND));
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public AutomationRuleShowDTO create(@Valid @RequestBody AutomationRulePostDTO rule,
                                        HttpServletRequest request) {
        User user = requireSettingsView(request);
        return AutomationRuleShowDTO.of(ruleService.create(rule, user.getCompany()));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public AutomationRuleShowDTO update(@PathVariable("id") Long id,
                                        @Valid @RequestBody AutomationRulePostDTO rule,
                                        HttpServletRequest request) {
        User user = requireSettingsView(request);
        return AutomationRuleShowDTO.of(ruleService.update(id, rule, user.getCompany()));
    }

    @PatchMapping("/{id}/enabled")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public AutomationRuleShowDTO setEnabled(@PathVariable("id") Long id,
                                            @Parameter(description = "New state") @RequestParam boolean enabled,
                                            HttpServletRequest request) {
        User user = requireSettingsView(request);
        return AutomationRuleShowDTO.of(ruleService.setEnabled(id, enabled, user.getCompany()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> delete(@PathVariable("id") Long id, HttpServletRequest request) {
        User user = requireSettingsView(request);
        ruleService.delete(id, user.getCompany());
        return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"), HttpStatus.OK);
    }

    /**
     * The execution history of one rule, newest first — including the runs that decided not to
     * do anything, which is the half that answers "why did my rule not fire?".
     */
    @GetMapping("/{id}/runs")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public Page<AutomationRunShowDTO> getRuns(@PathVariable("id") Long id,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size,
                                              HttpServletRequest request) {
        User user = requireSettingsView(request);
        // Through the rule, so a run of another company's rule cannot be read by guessing an id.
        ruleService.findByIdAndCompany(id, user.getCompany().getId())
                .orElseThrow(() -> new CustomException("Rule not found", HttpStatus.NOT_FOUND));
        return runService.findByRule(id, PageRequest.of(page, Math.min(size, 100)))
                .map(AutomationRunShowDTO::of);
    }

    @GetMapping("/runs")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public Page<AutomationRunShowDTO> getAllRuns(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size,
                                                 HttpServletRequest request) {
        User user = requireSettingsView(request);
        return runService.findByCompany(user.getCompany().getId(),
                        PageRequest.of(page, Math.min(size, 100)))
                .map(AutomationRunShowDTO::of);
    }

    private User requireSettingsView(HttpServletRequest request) {
        User user = userService.whoami(request);
        if (!user.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS)) {
            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        }
        return user;
    }
}
