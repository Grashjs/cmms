package com.grash.controller;

import com.grash.dto.*;
import com.grash.mapper.MultiPartsMapper;
import com.grash.model.MultiParts;
import com.grash.model.User;
import com.grash.security.CurrentUser;
import com.grash.service.MultiPartsService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.Collection;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/multi-parts")
@Tag(name = "Multi Parts", description = "Operations on multi parts")
@RequiredArgsConstructor
public class MultiPartsController {

    private final MultiPartsService multiPartsService;
    private final MultiPartsMapper multiPartsMapper;

    @GetMapping("")
    @PreAuthorize("permitAll()")
    public Collection<MultiPartsShowDTO> getAll(@Parameter(hidden = true) @CurrentUser User user) {
        return multiPartsService.getAll(user).stream().map(multiPartsMapper::toShowDto).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public MultiPartsShowDTO getById(@PathVariable("id") Long id,
                                     @Parameter(hidden = true) @CurrentUser User user) {
        return multiPartsMapper.toShowDto(multiPartsService.getById(id, user));
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    MultiPartsShowDTO create(@Parameter(description = "Multi-part to create") @Valid @RequestBody MultiParts multiPartsReq,
                             @Parameter(hidden = true) @CurrentUser User user) {
        return multiPartsMapper.toShowDto(multiPartsService.create(multiPartsReq, user));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public MultiPartsShowDTO patch(@Parameter(description = "Multi-part fields to update") @Valid @RequestBody MultiPartsPatchDTO multiParts,
                                   @PathVariable("id") Long id,
                                   @Parameter(hidden = true) @CurrentUser User user) {
        return multiPartsMapper.toShowDto(multiPartsService.patch(id, multiParts, user));
    }

    @GetMapping("/mini")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public Collection<MultiPartsMiniDTO> getMini(@Parameter(hidden = true) @CurrentUser User user) {
        return multiPartsService.findByCompany(user.getCompany().getId()).stream().map(multiPartsMapper::toMiniDto).collect(Collectors.toList());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> delete(@PathVariable("id") Long id,
                                                  @Parameter(hidden = true) @CurrentUser User user) {
        multiPartsService.deleteByIdAndUser(id, user);
        return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"),
                HttpStatus.OK);
    }
}