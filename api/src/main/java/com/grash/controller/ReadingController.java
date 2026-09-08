package com.grash.controller;

import com.grash.dto.DateRange;
import com.grash.dto.ReadingHistogramDTO;
import com.grash.dto.ReadingPatchDTO;
import com.grash.dto.SuccessResponse;
import com.grash.model.Reading;
import com.grash.model.User;
import com.grash.security.CurrentUser;
import com.grash.service.ReadingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/readings")
@Tag(name = "Readings", description = "Operations on meter readings")
@RequiredArgsConstructor
public class ReadingController {

    private final ReadingService readingService;


    @GetMapping("/meter/{id}")
    @PreAuthorize("permitAll()")
    public Collection<Reading> getByMeter(@PathVariable("id") Long id,
                                          @Parameter(hidden = true) @CurrentUser User user) {
        return readingService.getByMeter(id, user);
    }

    @PostMapping("/meter/{id}/histogram")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Get histogram data for a meter within a date range (max 30 points)")
    public List<ReadingHistogramDTO> getHistogram(
            @PathVariable("id") Long id,
            @RequestBody DateRange dateRange,
            @Parameter(hidden = true) @CurrentUser User user) {
        return readingService.getHistogram(id, dateRange, user);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    Reading create(@Parameter(description = "Reading data to create") @Valid @RequestBody Reading readingReq,
                   @Parameter(hidden = true) @CurrentUser User user) {
        return readingService.create(readingReq, user);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public Reading patch(@Parameter(description = "Reading fields to update") @Valid @RequestBody ReadingPatchDTO reading,
                         @PathVariable("id") Long id,
                         @Parameter(hidden = true) @CurrentUser User user) {
        return readingService.patch(id, reading, user);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> delete(@PathVariable("id") Long id,
                                                  @Parameter(hidden = true) @CurrentUser User user) {
        readingService.deleteByIdAndUser(id, user);
        return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"),
                HttpStatus.OK);
    }
}