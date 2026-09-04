package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.MeterMiniDTO;
import com.grash.dto.MeterPatchDTO;
import com.grash.dto.MeterPostDTO;
import com.grash.dto.MeterShowDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.mapper.MeterMapper;
import com.grash.model.Asset;
import com.grash.model.User;
import com.grash.security.CurrentUser;
import com.grash.service.AssetService;
import com.grash.service.MeterService;
import com.grash.service.ReadingService;
import com.grash.utils.TenantAspectUtils;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/meters")
@Tag(name = "Meters", description = "Operations on meters")
@RequiredArgsConstructor
public class MeterController {

    private final MeterService meterService;
    private final MeterMapper meterMapper;
    private final AssetService assetService;
    private final ReadingService readingService;

    @PostMapping("/search")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<MeterShowDTO>> search(@Parameter(description = "Search criteria for filtering meters") @RequestBody SearchCriteria searchCriteria,
                                                     @Parameter(hidden = true) @CurrentUser User user) {
        return ResponseEntity.ok(meterService.findBySearchCriteria(meterService.getSearchCriteria(user, searchCriteria))
                .map(meter -> meterMapper.toShowDto(meter, readingService)
                ));
    }

    @GetMapping("/mini")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public Collection<MeterMiniDTO> getMini(@Parameter(hidden = true) @CurrentUser User user) {
        return meterService.findByCompany(user.getCompany().getId()).stream().map(meterMapper::toMiniDto).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public MeterShowDTO getById(@Parameter(description = "Meter ID") @PathVariable("id") Long id,
                                @Parameter(hidden = true) @CurrentUser User user) {
        return meterMapper.toShowDto(meterService.getById(id, user), readingService);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    MeterShowDTO create(@Parameter(description = "Meter data to create") @Valid @RequestBody MeterPostDTO meterReq,
                        @Parameter(hidden = true) @CurrentUser User user) {
        return meterMapper.toShowDto(meterService.create(meterReq, user), readingService);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public MeterShowDTO patch(@Parameter(description = "Meter fields to update") @Valid @RequestBody MeterPatchDTO meter,
                              @Parameter(description = "Meter ID") @PathVariable("id") Long id,
                              @Parameter(hidden = true) @CurrentUser User user) {
        return meterMapper.toShowDto(meterService.patch(id, meter, user), readingService);
    }

    @GetMapping("/asset/{id}")
    @PreAuthorize("permitAll()")
    public Collection<MeterShowDTO> getByAsset(@Parameter(description = "Asset ID") @PathVariable("id") Long id,
                                               @Parameter(hidden = true) @CurrentUser User user) {
        Optional<Asset> optionalAsset = assetService.findById(id);
        if (optionalAsset.isPresent()) {
            if (!optionalAsset.get().canBeViewedBy(user))
                throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            return meterService.findByAsset(id).stream().map(meter -> meterMapper.toShowDto(meter, readingService)).collect(Collectors.toList());
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> delete(@Parameter(description = "Meter ID") @PathVariable("id") Long id,
                                                  @Parameter(hidden = true) @CurrentUser User user) {
        meterService.deleteByIdAndUser(id, user);
        return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"),
                HttpStatus.OK);
    }
}
