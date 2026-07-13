package com.grash.controller;

import com.grash.dto.WorkOrderHistoryShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.WorkOrderHistoryMapper;
import com.grash.model.OwnUser;
import com.grash.model.WorkOrder;
import com.grash.model.WorkOrderHistory;
import com.grash.service.UserService;
import com.grash.service.WorkOrderHistoryService;
import com.grash.service.WorkOrderService;
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
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/work-order-histories")
@Api(tags = "workOrderHistory")
@RequiredArgsConstructor
public class WorkOrderHistoryController {

    private final WorkOrderHistoryService workOrderHistoryService;
    private final UserService userService;
    private final WorkOrderService workOrderService;
    private final WorkOrderHistoryMapper workOrderHistoryMapper;

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "WorkOrderHistory not found")})
    public WorkOrderHistoryShowDTO getById(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<WorkOrderHistory> optionalWorkOrderHistory = workOrderHistoryService.findById(id);
        if (optionalWorkOrderHistory.isPresent()) {
            WorkOrderHistory savedWorkOrderHistory = optionalWorkOrderHistory.get();
            return workOrderHistoryMapper.toShowDto(savedWorkOrderHistory);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/work-order/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "WorkOrderHistory not found")})
    public Collection<WorkOrderHistoryShowDTO> getByWorkOrder(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<WorkOrder> optionalWorkOrder = workOrderService.findById(id);
        if (optionalWorkOrder.isPresent()) {
            return workOrderHistoryService.findByWorkOrder(id).stream().map(workOrderHistoryMapper::toShowDto).collect(Collectors.toList());
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

}
