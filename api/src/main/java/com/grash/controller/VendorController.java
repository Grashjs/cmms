package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.SuccessResponse;
import com.grash.dto.VendorMiniDTO;
import com.grash.dto.VendorPatchDTO;
import com.grash.dto.VendorPostDTO;
import com.grash.dto.VendorShowDTO;
import com.grash.mapper.VendorMapper;
import com.grash.model.User;
import com.grash.security.CurrentUser;
import com.grash.service.VendorService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.Collection;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/vendors")
@Tag(name = "Vendors", description = "Operations on vendors")
@RequiredArgsConstructor
public class VendorController {

    private final VendorService vendorService;
    private final VendorMapper vendorMapper;

    @PostMapping("/search")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<VendorShowDTO>> search(@Parameter(description = "Search criteria for filtering " +
            "vendors") @RequestBody SearchCriteria searchCriteria,
            @Parameter(hidden = true) @CurrentUser User user) {
        return ResponseEntity.ok(vendorService.findBySearchCriteria(vendorService.getSearchCriteria(user,
                searchCriteria)).map(vendorMapper::toShowDto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public VendorShowDTO getById(@PathVariable("id") Long id,
                                 @Parameter(hidden = true) @CurrentUser User user) {
        return vendorMapper.toShowDto(vendorService.getById(id, user));
    }

    @GetMapping("/mini")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public Collection<VendorMiniDTO> getMini(@Parameter(hidden = true) @CurrentUser User user) {
        return vendorService.findByCompany(user.getCompany().getId()).stream().map(vendorMapper::toMiniDto).collect(Collectors.toList());
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    VendorShowDTO create(@Parameter(description = "Vendor data to create") @Valid @RequestBody VendorPostDTO vendorReq,
                         @Parameter(hidden = true) @CurrentUser User user) {
        return vendorMapper.toShowDto(vendorService.create(vendorReq, user));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public VendorShowDTO patch(@Parameter(description = "Vendor fields to update") @Valid @RequestBody VendorPatchDTO vendor,
                               @PathVariable("id") Long id,
                               @Parameter(hidden = true) @CurrentUser User user) {
        return vendorMapper.toShowDto(vendorService.patch(id, vendor, user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> delete(@PathVariable("id") Long id,
                                                  @Parameter(hidden = true) @CurrentUser User user) {
        vendorService.deleteByIdAndUser(id, user);
        return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"),
                HttpStatus.OK);
    }

}


