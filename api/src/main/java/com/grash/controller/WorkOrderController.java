package com.grash.controller;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.*;
import com.grash.dto.workOrder.WorkOrderPatchDTO;
import com.grash.dto.workOrder.WorkOrderPostDTO;
import com.grash.dto.workOrder.WorkOrderSendReportDTO;
import com.grash.dto.workOrder.WorkOrderShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.FileMapper;
import com.grash.mapper.WorkOrderMapper;
import com.grash.model.*;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PlanFeatures;
import com.grash.model.enums.RoleType;
import com.grash.service.*;
import com.grash.utils.TenantAspectUtils;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/work-orders")
@Tag(name = "Work Orders", description = "Operations on work orders")
@RequiredArgsConstructor
@Slf4j
public class WorkOrderController {

    private final WorkOrderService workOrderService;
    private final WorkOrderMapper workOrderMapper;
    private final UserService userService;
    private final AssetService assetService;
    private final LocationService locationService;
    private final PartService partService;
    private final FileMapper fileMapper;

    @PostMapping("/search")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<WorkOrderShowDTO>> search(@Parameter(description = "Search criteria for filtering work" +
                                                                 " orders") @RequestBody SearchCriteria searchCriteria,
                                                         HttpServletRequest req) {
        User user = userService.whoami(req);
        return ResponseEntity.ok(TenantAspectUtils.executeWithDisabledCompanyCheck(() ->
                workOrderService.findBySearchCriteria(workOrderService.getSearchCriteria(user,
                        searchCriteria)).map(workOrderMapper::toShowDto)
        ));
    }

    @PostMapping("/search/mini")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<WorkOrderBaseMiniDTO>> searchMini(@Parameter(description = "Search criteria for " +
                                                                         "filtering work orders") @RequestBody SearchCriteria searchCriteria,
                                                                 HttpServletRequest req) {
        User user = userService.whoami(req);
        return ResponseEntity.ok(TenantAspectUtils.executeWithDisabledCompanyCheck(() ->
                workOrderService.findBySearchCriteria(workOrderService.getSearchCriteria(user,
                                searchCriteria))
                        .map(workOrderMapper::toBaseMiniDto)
        ));
    }

    @PostMapping("/events")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public Collection<CalendarEvent<WorkOrderBaseMiniDTO>> getEvents(@Parameter(description = "Date range for " +
            "calendar events") @Valid @RequestBody DateRange
                                                                             dateRange, @RequestParam(required =
            false) Long companyId, HttpServletRequest req) {
        User user = userService.whoami(req);
        return workOrderService.getEvents(dateRange, companyId, user);
    }

    @GetMapping("/asset/{id}")
    @PreAuthorize("permitAll()")
    public Collection<WorkOrderShowDTO> getByAsset(@PathVariable("id") Long id,
                                                   HttpServletRequest req) {
        User user = userService.whoami(req);
        Optional<Asset> optionalAsset = assetService.findById(id);
        if (optionalAsset.isPresent()) {
            return workOrderService.findByAsset(id).stream().map(workOrderMapper::toShowDto).collect(Collectors.toList());
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/location/{id}")
    @PreAuthorize("permitAll()")
    public Collection<WorkOrderShowDTO> getByLocation(@PathVariable("id") Long id,
                                                      HttpServletRequest req) {
        User user = userService.whoami(req);
        Optional<Location> optionalLocation = locationService.findById(id);
        if (optionalLocation.isPresent()) {
            return workOrderService.findByLocation(id).stream().map(workOrderMapper::toShowDto).collect(Collectors.toList());
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public WorkOrderShowDTO getById(@PathVariable("id") Long id, HttpServletRequest req) {
        User user = userService.whoami(req);
        return workOrderMapper.toShowDto(workOrderService.checkAccessToWorkOrderId(id, user));
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    WorkOrderShowDTO create(@Parameter(description = "Work order data to create") @Valid @RequestBody WorkOrderPostDTO
                                    workOrderReq, HttpServletRequest req) {
        User user = userService.whoami(req);
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.WORK_ORDERS)
                && (workOrderReq.getSignature() == null ||
                user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.SIGNATURE))) {
            if (user.getCompany().getCompanySettings().getGeneralPreferences().isAutoAssignWorkOrders()) {
                User primaryUser = workOrderReq.getPrimaryUser();
                workOrderReq.setPrimaryUser(primaryUser == null ? user : primaryUser);
            }
            WorkOrder createdWorkOrder = workOrderService.createWithIntercom(workOrderReq, user);
            return workOrderMapper.toShowDto(createdWorkOrder);
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/part/{id}")
    @PreAuthorize("permitAll()")
    public Collection<WorkOrderShowDTO> getByPart(@PathVariable("id") Long id, HttpServletRequest req) {
        User user = userService.whoami(req);
        Optional<Part> optionalPart = partService.findById(id);
        if (optionalPart.isPresent()) {
            return workOrderService.getWorkOrdersByPart(id).stream().map(workOrderMapper::toShowDto).collect(Collectors.toList());
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public WorkOrderShowDTO patch(@Parameter(description = "Work order fields to update") @Valid @RequestBody WorkOrderPatchDTO
                                          workOrder, @PathVariable("id") Long id,
                                  HttpServletRequest req) {
        User user = userService.whoami(req);
        WorkOrder patchedWorkOrder = workOrderService.patch(id, workOrder, user);
        return workOrderMapper.toShowDto(patchedWorkOrder);
    }

    @PatchMapping("/{id}/change-status")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public WorkOrderShowDTO changeStatus(@Parameter(description = "Work order status change data") @Valid @RequestBody WorkOrderChangeStatusDTO
                                                 workOrder, @PathVariable("id") Long id,
                                         HttpServletRequest req,
                                         @RequestHeader(value = "X-Platform", required = false) String platform) {
        User user = userService.whoami(req);
        WorkOrder patchedWorkOrder = workOrderService.changeStatus(workOrder, id, user, platform);
        return workOrderMapper.toShowDto(patchedWorkOrder);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> delete(@PathVariable("id") Long id, HttpServletRequest req) {
        User user = userService.whoami(req);
        workOrderService.deleteByIdAndUser(id, user);
        return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"),
                HttpStatus.OK);
    }

    @GetMapping(path = "/report/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Deprecated
    public ResponseEntity<SuccessResponse> getPDF(@PathVariable("id") Long id, HttpServletRequest req) {
        User user = userService.whoami(req);
        String signedUrl = workOrderService.generateReport(id, user, new ReportConfig());
        return ResponseEntity.ok().body(new SuccessResponse(true, signedUrl));
    }

    @PostMapping(path = "/report/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> getPDFWithConfig(@PathVariable("id") Long id,
                                                            @Valid @RequestBody ReportConfig config,
                                                            HttpServletRequest req) {
        User user = userService.whoami(req);
        String signedUrl = workOrderService.generateReport(id, user, config);
        return ResponseEntity.ok().body(new SuccessResponse(true, signedUrl));
    }

    @PostMapping("/{id}/report/send")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> sendReport(@PathVariable("id") Long id,
                                                      @Valid @RequestBody WorkOrderSendReportDTO request,
                                                      HttpServletRequest req) {
        User user = userService.whoami(req);
        workOrderService.sendReport(id, request, user);
        return ResponseEntity.ok(new SuccessResponse(true, "Report sent successfully"));
    }

    @GetMapping("/urgent")
    @PreAuthorize("permitAll()")
    public SuccessResponse getUrgentCount(HttpServletRequest req) {
        User user = userService.whoami(req);
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT) && user.getRole().getViewPermissions().contains(PermissionEntity.REQUESTS)) {
            return new SuccessResponse(true, workOrderService.countUrgent(user).toString());
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    @PatchMapping("/files/{id}/add")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public List<FileShowDTO> addFilesToWorkOrder(@PathVariable("id") Long id, @Parameter(description = "List of files" +
                                                         " to " +
                                                         "add") @RequestBody List<File> files,
                                                 HttpServletRequest req) {
        User user = userService.whoami(req);
        return workOrderService.addFiles(id, files, user).stream().map(fileMapper::toShowDto).collect(Collectors.toList());
    }

    @DeleteMapping("/files/{id}/{fileId}/remove")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public List<FileShowDTO> removeFileFromWorkOrder(@PathVariable("id") Long id,
                                                     @PathVariable("fileId") Long fileId, HttpServletRequest req) {
        User user = userService.whoami(req);
        return workOrderService.removeFile(id, fileId, user).stream().map(fileMapper::toShowDto).collect(Collectors.toList());
    }

}