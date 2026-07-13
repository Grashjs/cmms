package com.grash.controller;

import com.grash.exception.CustomException;
import com.grash.model.OwnUser;
import com.grash.model.WorkOrderRequestConfiguration;
import com.grash.service.UserService;
import com.grash.service.WorkOrderRequestConfigurationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@RestController
@RequestMapping("/work-order-request-configurations")
@Api(tags = "workOrderRequestConfiguration")
@RequiredArgsConstructor
public class WorkOrderRequestConfigurationController {

    private final WorkOrderRequestConfigurationService workOrderRequestConfigurationService;
    private final UserService userService;

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "WorkOrderRequestConfiguration not found")})
    public WorkOrderRequestConfiguration getById(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<WorkOrderRequestConfiguration> optionalWorkOrderRequestConfiguration = workOrderRequestConfigurationService.findById(id);
        if (optionalWorkOrderRequestConfiguration.isPresent()) {
            WorkOrderRequestConfiguration savedWorkOrderRequestConfiguration = optionalWorkOrderRequestConfiguration.get();
            return savedWorkOrderRequestConfiguration;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

}
