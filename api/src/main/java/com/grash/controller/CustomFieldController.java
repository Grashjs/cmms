package com.grash.controller;

import com.grash.dto.CustomFieldPatchDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.CustomField;
import com.grash.model.OwnUser;
import com.grash.service.CustomFieldService;
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
import java.util.Optional;

@RestController
@RequestMapping("/custom-fields")
@Api(tags = "customField")
@RequiredArgsConstructor
public class CustomFieldController {

    private final CustomFieldService customFieldService;
    private final UserService userService;

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "CustomField not found")})
    public CustomField getById(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<CustomField> optionalCustomField = customFieldService.findById(id);
        if (optionalCustomField.isPresent()) {
            CustomField savedCustomField = optionalCustomField.get();
            return savedCustomField;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied")})
    public CustomField create(@ApiParam("CustomField") @Valid @RequestBody CustomField customFieldReq, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        return customFieldService.create(customFieldReq);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "CustomField not found")})
    public CustomField patch(@ApiParam("CustomField") @Valid @RequestBody CustomFieldPatchDTO customField, @ApiParam("id") @PathVariable("id") Long id,
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
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "CustomField not found")})
    public ResponseEntity delete(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
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
