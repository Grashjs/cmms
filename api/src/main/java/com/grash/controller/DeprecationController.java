package com.grash.controller;

import com.grash.dto.DeprecationPatchDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.Deprecation;
import com.grash.model.OwnUser;
import com.grash.service.DeprecationService;
import com.grash.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.Optional;

@RestController
@RequestMapping("/deprecations")
@Tag(name = "deprecation")
@RequiredArgsConstructor
public class DeprecationController {

    private final DeprecationService deprecationService;
    private final UserService userService;

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public Deprecation getById(@PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Deprecation> optionalDeprecation = deprecationService.findById(id);
        if (optionalDeprecation.isPresent()) {
            Deprecation savedDeprecation = optionalDeprecation.get();
            return savedDeprecation;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public Deprecation patch(@Valid @RequestBody DeprecationPatchDTO deprecation, @PathVariable("id") Long id,
                             HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Deprecation> optionalDeprecation = deprecationService.findById(id);

        if (optionalDeprecation.isPresent()) {
            Deprecation savedDeprecation = optionalDeprecation.get();
            return deprecationService.update(id, deprecation);
        } else throw new CustomException("Deprecation not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity delete(@PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<Deprecation> optionalDeprecation = deprecationService.findById(id);
        if (optionalDeprecation.isPresent()) {
            Deprecation savedDeprecation = optionalDeprecation.get();
            deprecationService.delete(id);
            return new ResponseEntity(new SuccessResponse(true, "Deleted successfully"),
                    HttpStatus.OK);
        } else throw new CustomException("Deprecation not found", HttpStatus.NOT_FOUND);
    }


}


