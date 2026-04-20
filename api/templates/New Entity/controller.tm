package com.grash.controller;

import com.grash.dto.SuccessResponse;
import com.grash.dto.{name}[-c].{name}[-C]Criteria;
import com.grash.dto.{name}[-c].{name}[-C]PatchDTO;
import com.grash.dto.{name}[-c].{name}[-C]PostDTO;
import com.grash.dto.{name}[-c].{name}[-C]ShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.{name}[-C]Mapper;
import com.grash.model.{name}[-C];
import com.grash.model.User;
import com.grash.security.CurrentUser;
import com.grash.service.{name}[-C]Service;
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

import java.util.Optional;

@RestController
@RequestMapping("/{name}[-d]s")
@RequiredArgsConstructor
public class {name}[-C]Controller {

    private final {name}[-C]Service {name}[-c]Service;
    private final {name}[-C]Mapper {name}[-c]Mapper;

    @PostMapping("/search")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public Page<{name}[-C]ShowDTO> search(@RequestBody {name}[-C]Criteria {name}[-c]Criteria, @Parameter(hidden = true) @CurrentUser User user, Pageable pageable) {
        return {name}[-c]Service.findByCriteria({name}[-c]Criteria, pageable, user).map({name}[-c]Mapper::toShowDto);
    }


    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public {name}[-C]ShowDTO create(@RequestBody @Valid {name}[-C]PostDTO {name}[-c],
                                         @Parameter(hidden = true) @CurrentUser User user) {
        return {name}[-c]Mapper.toShowDto({name}[-c]Service.create({name}[-c], user));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public {name}[-C]ShowDTO getById(@PathVariable Long id, @Parameter(hidden = true) @CurrentUser User user) {
        return {name}[-c]Mapper.toShowDto({name}[-c]Service.findById(id).orElseThrow(() -> new CustomException("Not found",
                HttpStatus.NOT_FOUND)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public {name}[-C]ShowDTO update(@PathVariable Long id,
                                         @RequestBody @Valid {name}[-C]PatchDTO {name}[-c],
                                         @Parameter(hidden = true) @CurrentUser User user) {
        return {name}[-c]Mapper.toShowDto({name}[-c]Service.update(id, {name}[-c], user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> delete(@PathVariable("id") Long id,
                                                  @Parameter(hidden = true) @CurrentUser User user) {

        {name}[-C] saved{name}[-C] =
                {name}[-c]Service.findById(id).orElseThrow(() -> new CustomException("Not found",
                        HttpStatus.NOT_FOUND));
        {name}[-c]Service.delete(id);
        return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"), HttpStatus.OK);
    }
}
