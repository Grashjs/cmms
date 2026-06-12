package com.grash.controller;

import com.grash.dto.license.LicenseEntitlement;
import com.grash.dto.workOrder.WorkOrderScheduleDTO;
import com.grash.dto.workload.UnscheduledWorkOrdersDTO;
import com.grash.dto.workload.WorkloadOverviewDTO;
import com.grash.dto.workload.WorkloadScheduleDTO;
import com.grash.exception.CustomException;
import com.grash.model.User;
import com.grash.model.WorkOrder;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PlanFeatures;
import com.grash.model.enums.Status;
import com.grash.service.LicenseService;
import com.grash.service.UserService;
import com.grash.service.WorkloadService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/workload")
@Tag(name = "Workload", description = "Resource planning and workload view")
@RequiredArgsConstructor
public class WorkloadController {

    private final WorkloadService workloadService;
    private final UserService userService;
    private final LicenseService licenseService;

    @GetMapping("/overview")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public WorkloadOverviewDTO getOverview(
            @Parameter(description = "Start date (ISO format)") @RequestParam LocalDate startDate,
            @Parameter(description = "End date (ISO format)") @RequestParam LocalDate endDate,
            @Parameter(description = "Comma-separated user IDs") @RequestParam(required = false) List<Long> userIds,
            HttpServletRequest req) {
        User user = userService.whoami(req);
        checkAccess(user);
        if (userIds == null || userIds.isEmpty()) {
            userIds = userService.findWorkersByCompany(user.getCompany().getId())
                    .stream().map(User::getId).toList();
        }
        return workloadService.getOverview(startDate, endDate, userIds, user.getCompany().getId());
    }

    @GetMapping("/unscheduled")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public UnscheduledWorkOrdersDTO getUnscheduled(
            @Parameter(description = "Comma-separated statuses to filter by") @RequestParam(required = false) List<Status> statuses,
            HttpServletRequest req) {
        User user = userService.whoami(req);
        checkAccess(user);
        return workloadService.getUnscheduled(user.getCompany().getId(), statuses);
    }

    @PatchMapping("/work-orders/{id}/schedule")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public WorkOrderScheduleDTO scheduleWorkOrder(
            @Parameter(description = "Work order ID") @PathVariable Long id,
            @Valid @RequestBody WorkloadScheduleDTO dto,
            HttpServletRequest req) {
        User user = userService.whoami(req);
        checkAccess(user);
        WorkOrder workOrder = workloadService.scheduleWorkOrder(id, dto, user);
        WorkOrderScheduleDTO responseDto = new WorkOrderScheduleDTO();
        User primaryUser = workOrder.getPrimaryUser();
        responseDto.setUserId(primaryUser == null ? null : primaryUser.getId());
        responseDto.setUserFirstName(primaryUser == null ? null : primaryUser.getFirstName());
        responseDto.setUserLastName(primaryUser == null ? null : primaryUser.getLastName());
        responseDto.setEstimatedStartDate(workOrder.getEstimatedStartDate());
        return responseDto;
    }

    private void checkAccess(User user) {
        if (!licenseService.hasEntitlement(LicenseEntitlement.RESOURCE_PLANNING)) {
            throw new CustomException("You need a license for resource planning", HttpStatus.FORBIDDEN);
        }
        if (!user.getRole().getViewOtherPermissions().contains(PermissionEntity.WORK_ORDERS) || !user.getRole().getEditOtherPermissions().contains(PermissionEntity.PEOPLE_AND_TEAMS)) {
            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        }
        if (!user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.RESOURCE_PLANNING)) {
            throw new CustomException("Your plan does not include resource planning", HttpStatus.FORBIDDEN);
        }
    }
}
