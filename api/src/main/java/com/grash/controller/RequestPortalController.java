package com.grash.controller;

import com.grash.dto.SuccessResponse;
import com.grash.dto.requestPortal.*;
import com.grash.exception.CustomException;
import com.grash.mapper.RequestPortalMapper;
import com.grash.model.RequestPortal;
import com.grash.model.User;
import com.grash.security.CurrentUser;
import com.grash.service.RequestPortalService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/request-portals")
@RequiredArgsConstructor
@Tag(name = "Request portals", description = "Operations on request portals")
public class RequestPortalController {

    private final RequestPortalService requestPortalService;
    private final RequestPortalMapper requestPortalMapper;

    @PostMapping("/search")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public Page<RequestPortalShowDTO> search(@Parameter(description = "Request portal search criteria") @RequestBody RequestPortalCriteria requestPortalCriteria,
                                             @Parameter(hidden = true) @CurrentUser User user, Pageable pageable) {
        return requestPortalService.findByCriteria(requestPortalCriteria, pageable, user).map(requestPortalMapper::toShowDto);
    }


    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public RequestPortalShowDTO create(@Parameter(description = "Request portal to create") @RequestBody @Valid RequestPortalPostDTO requestPortal,
                                       @Parameter(hidden = true) @CurrentUser User user) {
        return requestPortalMapper.toShowDto(requestPortalService.create(requestPortal, user));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public RequestPortalShowDTO getById(@PathVariable Long id, @Parameter(hidden = true) @CurrentUser User user) {
        return requestPortalMapper.toShowDto(requestPortalService.findById(id).orElseThrow(() -> new CustomException(
                "Not found",
                HttpStatus.NOT_FOUND)));
    }

    @GetMapping("/public/{uuid}")
    public RequestPortalPublicDTO getByIdPublic(@PathVariable String uuid) {
        return requestPortalMapper.toPublicDto(requestPortalService.findByUuidByUser(uuid).orElseThrow(() -> new CustomException(
                "Not found",
                HttpStatus.NOT_FOUND)));
    }


    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public RequestPortalShowDTO update(@PathVariable Long id,
                                       @Parameter(description = "Request portal fields to update") @RequestBody @Valid RequestPortalPatchDTO requestPortal,
                                       @Parameter(hidden = true) @CurrentUser User user) {
        return requestPortalMapper.toShowDto(requestPortalService.update(id, requestPortal, user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> delete(@PathVariable("id") Long id,
                                                  @Parameter(hidden = true) @CurrentUser User user) {

        RequestPortal savedRequestPortal =
                requestPortalService.findById(id).orElseThrow(() -> new CustomException("Not found",
                        HttpStatus.NOT_FOUND));
        requestPortalService.delete(id);
        return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"), HttpStatus.OK);
    }
}
