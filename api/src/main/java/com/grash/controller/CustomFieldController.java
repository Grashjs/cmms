package com.grash.controller;

import com.grash.dto.CustomFieldPatchDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.CustomField;
import com.grash.model.OwnUser;
import com.grash.service.CustomFieldService;
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
@RequestMapping("/custom-fields")
@Tag(name = "customField")
@RequiredArgsConstructor
public class CustomFieldController {

    private final CustomFieldService customFieldService;
    private final UserService userService;

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public CustomField getById(@PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<CustomField> optionalCustomField = customFieldService.findById(id);
        if (optionalCustomField.isPresent()) {
            CustomField savedCustomField = optionalCustomField.get();
            return savedCustomField;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public CustomField patch(@Valid @RequestBody CustomFieldPatchDTO customField, @PathVariable("id") Long id,
                             HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<CustomField> optionalCustomField = customFieldService.findById(id);

        if (optionalCustomField.isPresent()) {
            CustomField savedCustomField = optionalCustomField.get();
            return customFieldService.update(id, customField);
        } else throw new CustomException("CustomField not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity delete(@PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<CustomField> optionalCustomField = customFieldService.findById(id);
        if (optionalCustomField.isPresent()) {
            CustomField savedCustomField = optionalCustomField.get();
            customFieldService.delete(id);
            return new ResponseEntity(new SuccessResponse(true, "Deleted successfully"),
                    HttpStatus.OK);
        } else throw new CustomException("CustomField not found", HttpStatus.NOT_FOUND);
    }
}


