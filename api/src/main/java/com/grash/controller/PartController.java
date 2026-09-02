package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.PartMiniDTO;
import com.grash.dto.PartPatchDTO;
import com.grash.dto.PartPostDTO;
import com.grash.dto.PartRestockDTO;
import com.grash.dto.PartShowDTO;
import com.grash.dto.SuccessResponse;
import com.grash.mapper.PartMapper;
import com.grash.model.User;
import com.grash.security.CurrentUser;
import com.grash.service.PartService;
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

import java.util.Collection;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/parts")
@Tag(name = "Parts", description = "Operations on parts")
@RequiredArgsConstructor
public class PartController {

    private final PartService partService;
    private final PartMapper partMapper;

    @PostMapping("/search")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<PartShowDTO>> search(@Parameter(description = "Search criteria for filtering parts") @RequestBody SearchCriteria searchCriteria,
                                                    @Parameter(hidden = true) @CurrentUser User user) {
        return ResponseEntity.ok(TenantAspectUtils.executeWithDisabledCompanyCheck(() ->
                partService.findBySearchCriteria(partService.getSearchCriteria(user,
                        searchCriteria)).map(partMapper::toShowDto)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public PartShowDTO getById(@Parameter(description = "Part ID") @PathVariable("id") Long id,
                               @Parameter(hidden = true) @CurrentUser User user) {
        return partMapper.toShowDto(partService.getById(id, user));
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    PartShowDTO create(@Parameter(description = "Part data to create") @Valid @RequestBody PartPostDTO partReq,
                       @Parameter(hidden = true) @CurrentUser User user) {
        return partMapper.toShowDto(partService.create(partReq, user));
    }

    @PostMapping("/{id}/restock")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> restock(@Parameter(description = "Part ID") @PathVariable("id") Long id,
                                                   @Valid @RequestBody PartRestockDTO partRestockDTO,
                                                   @Parameter(hidden = true) @CurrentUser User user) {
        partService.restock(id, partRestockDTO, user);
        return new ResponseEntity<>(new SuccessResponse(true, "Restocked successfully"), HttpStatus.OK);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public PartShowDTO patch(@Parameter(description = "Part fields to update") @Valid @RequestBody PartPatchDTO part,
                             @Parameter(description = "Part ID") @PathVariable(
                                     "id") Long id,
                             @Parameter(hidden = true) @CurrentUser User user) {
        return partMapper.toShowDto(partService.patch(id, part, user));
    }

    @GetMapping("/mini")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public Collection<PartMiniDTO> getMini(@Parameter(hidden = true) @CurrentUser User user) {
        return partService.getMini(user).stream().map(partMapper::toMiniDto).collect(Collectors.toList());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> delete(@Parameter(description = "Part ID") @PathVariable("id") Long id,
                                                  @Parameter(hidden = true) @CurrentUser User user) {
        partService.deleteByIdAndUser(id, user);
        return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"),
                HttpStatus.OK);
    }

}
