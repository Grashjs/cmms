package com.grash.controller;

import com.grash.dto.SuccessResponse;
import com.grash.dto.savedView.SavedViewPatchDTO;
import com.grash.dto.savedView.SavedViewPostDTO;
import com.grash.dto.savedView.SavedViewShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.SavedViewMapper;
import com.grash.model.SavedView;
import com.grash.model.User;
import com.grash.model.enums.SavedViewEntityType;
import com.grash.security.CurrentUser;
import com.grash.service.SavedViewService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

/**
 * Named list configurations. No entity-level permission is checked beyond ROLE_CLIENT: a saved
 * view holds no data, only a filter and a column layout, and applying it goes through the
 * entity's own search endpoint, which re-scopes it to what the user may see.
 */
@RestController
@RequestMapping("/saved-views")
@Tag(name = "Saved Views", description = "Named, reusable list configurations")
@RequiredArgsConstructor
public class SavedViewController {

    private final SavedViewService savedViewService;
    private final SavedViewMapper savedViewMapper;

    @GetMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public List<SavedViewShowDTO> getAll(@Parameter(hidden = true) @CurrentUser User user,
                                         @Parameter(description = "List page to fetch views for")
                                         @RequestParam SavedViewEntityType entityType) {
        return savedViewService.findVisible(user, entityType).stream()
                .map(savedView -> toShowDto(savedView, user))
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public SavedViewShowDTO getById(@PathVariable Long id,
                                    @Parameter(hidden = true) @CurrentUser User user) {
        SavedView savedView = savedViewService.findById(id)
                .orElseThrow(() -> new CustomException("Saved view not found", HttpStatus.NOT_FOUND));
        savedViewService.assertCanRead(savedView, user);
        return toShowDto(savedView, user);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public SavedViewShowDTO create(@Parameter(description = "View to create") @RequestBody @Valid SavedViewPostDTO dto,
                                   @Parameter(hidden = true) @CurrentUser User user) {
        return toShowDto(savedViewService.create(dto, user), user);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public SavedViewShowDTO patch(@PathVariable Long id,
                                  @Parameter(description = "Fields to change; null leaves a field unchanged")
                                  @RequestBody SavedViewPatchDTO dto,
                                  @Parameter(hidden = true) @CurrentUser User user) {
        return toShowDto(savedViewService.update(id, dto, user), user);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> delete(@PathVariable Long id,
                                                  @Parameter(hidden = true) @CurrentUser User user) {
        savedViewService.delete(id, user);
        return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"), HttpStatus.OK);
    }

    private SavedViewShowDTO toShowDto(SavedView savedView, User user) {
        SavedViewShowDTO result = savedViewMapper.toShowDto(savedView);
        result.setEditable(savedView.canBeEditedBy(user));
        return result;
    }
}
