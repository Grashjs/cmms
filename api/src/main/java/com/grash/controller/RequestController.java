package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.*;
import com.grash.dto.workOrder.WorkOrderShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.RequestMapper;
import com.grash.mapper.WorkOrderMapper;
import com.grash.model.Request;
import com.grash.model.User;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.security.CurrentUser;
import com.grash.service.RequestService;
import com.grash.utils.TenantAspectUtils;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/requests")
@Tag(name = "Requests", description = "Operations on maintenance requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;
    private final RequestMapper requestMapper;
    private final WorkOrderMapper workOrderMapper;

    @PostMapping("/search")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<RequestShowDTO>> search(@Parameter(description = "Search criteria for filtering " +
                                                               "requests") @RequestBody SearchCriteria searchCriteria,
                                                       @Parameter(hidden = true) @CurrentUser User user) {
        return ResponseEntity.ok(TenantAspectUtils.executeWithDisabledCompanyCheck(() ->
                requestService.findBySearchCriteria(requestService.getSearchCriteria(user,
                        searchCriteria)).map(requestMapper::toShowDto)
        ));
    }

    @GetMapping("/pending")
    @PreAuthorize("permitAll()")
    public SuccessResponse getPending(@Parameter(hidden = true) @CurrentUser User user) {
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT) && user.getRole().getViewPermissions().contains(PermissionEntity.REQUESTS)) {
            return new SuccessResponse(true, requestService.countPending(user.getCompany().getId()).toString());
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public RequestShowDTO getById(@PathVariable("id") Long id,
                                  @Parameter(hidden = true) @CurrentUser User user) {
        return requestMapper.toShowDto(requestService.getById(id, user));
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    RequestShowDTO create(@Parameter(description = "Request data to create") @Valid @RequestBody RequestPostDTO requestReq,
                          @Parameter(hidden = true) @CurrentUser User user) {
        return requestMapper.toShowDto(requestService.create(requestReq, user));
    }

    @PostMapping("/portal/{requestPortalUuid}")
    RequestShowDTO createFromPortal(@Parameter(description = "Request data to create from portal") @Valid @RequestBody Request requestReq,
                                    @PathVariable("requestPortalUuid") String requestPortalUuid,
                                    @RequestParam(value = "recaptchaToken", required = false) @Parameter(description
                                            = "RecaptchaToken value") String recaptchaToken) {
        return requestMapper.toShowDto(requestService.createFromPortal(requestReq, requestPortalUuid, recaptchaToken));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public RequestShowDTO patch(@Parameter(description = "Request fields to update") @Valid @RequestBody RequestPatchDTO request,
                                @PathVariable("id") Long id,
                                @Parameter(hidden = true) @CurrentUser User user) {
        return requestMapper.toShowDto(requestService.patch(id, request, user));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public WorkOrderShowDTO approve(@PathVariable("id") Long id,
                                    @Parameter(description = "Request approval data") @RequestBody RequestApproveDTO requestApproveDTO,
                                    @Parameter(hidden = true) @CurrentUser User user) {
        return workOrderMapper.toShowDto(requestService.approve(id, requestApproveDTO, user));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public RequestShowDTO cancel(@PathVariable("id") Long id,
                                 @RequestParam @Parameter(description = "Reason of the request") String reason,
                                 @Parameter(hidden = true) @CurrentUser User user) {
        return requestMapper.toShowDto(requestService.cancel(id, reason, user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> delete(@PathVariable("id") Long id,
                                                  @Parameter(hidden = true) @CurrentUser User user) {
        requestService.deleteByIdAndUser(id, user);
        return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"),
                HttpStatus.OK);
    }

}