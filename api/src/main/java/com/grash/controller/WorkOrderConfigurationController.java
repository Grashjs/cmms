package com.grash.controller;

import com.grash.exception.CustomException;
import com.grash.model.OwnUser;
import com.grash.model.WorkOrderConfiguration;
import com.grash.service.UserService;
import com.grash.service.WorkOrderConfigurationService;
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
@RequestMapping("/work-order-configurations")
@Api(tags = "workOrderConfiguration")
@RequiredArgsConstructor
public class WorkOrderConfigurationController {

    private final WorkOrderConfigurationService workOrderConfigurationService;
    private final UserService userService;

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "WorkOrderConfiguration not found")})
    public WorkOrderConfiguration getById(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<WorkOrderConfiguration> optionalWorkOrderConfiguration = workOrderConfigurationService.findById(id);
        if (optionalWorkOrderConfiguration.isPresent()) {
            WorkOrderConfiguration savedWorkOrderConfiguration = optionalWorkOrderConfiguration.get();
            return savedWorkOrderConfiguration;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

}
