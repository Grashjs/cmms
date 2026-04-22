package com.grash.controller;

import com.grash.exception.CustomException;
import com.grash.model.User;
import com.grash.model.WorkOrderRequestConfiguration;
import com.grash.service.UserService;
import com.grash.service.WorkOrderRequestConfigurationService;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

@RestController
@RequestMapping("/work-order-request-configurations")
@Tag(name = "Work Order Request Configuration", description = "Operations on work order request configuration")
@RequiredArgsConstructor
public class WorkOrderRequestConfigurationController {

    private final WorkOrderRequestConfigurationService workOrderRequestConfigurationService;
    private final UserService userService;

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")

    public WorkOrderRequestConfiguration getById(@PathVariable("id") Long id, HttpServletRequest req) {
        User user = userService.whoami(req);
        Optional<WorkOrderRequestConfiguration> optionalWorkOrderRequestConfiguration =
                workOrderRequestConfigurationService.findById(id);
        if (optionalWorkOrderRequestConfiguration.isPresent()) {
            WorkOrderRequestConfiguration savedWorkOrderRequestConfiguration =
                    optionalWorkOrderRequestConfiguration.get();
            if (!savedWorkOrderRequestConfiguration.getCompanySettings().getCompany().getId().equals(user.getCompany().getId())) {
                throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
            }
            return savedWorkOrderRequestConfiguration;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

}

