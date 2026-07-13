package com.grash.controller;

import com.grash.dto.SuccessResponse;
import com.grash.dto.WorkOrderMeterTriggerPatchDTO;
import com.grash.dto.WorkOrderMeterTriggerShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.WorkOrderMeterTriggerMapper;
import com.grash.model.Meter;
import com.grash.model.OwnUser;
import com.grash.model.WorkOrderMeterTrigger;
import com.grash.service.MeterService;
import com.grash.service.UserService;
import com.grash.service.WorkOrderMeterTriggerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/work-order-meter-triggers")
@Api(tags = "workOrderMeterTrigger")
@RequiredArgsConstructor
public class WorkOrderMeterTriggerController {

    private final WorkOrderMeterTriggerService workOrderMeterTriggerService;
    private final WorkOrderMeterTriggerMapper workOrderMeterTriggerMapper;
    private final UserService userService;
    private final MeterService meterService;


    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "WorkOrderMeterTrigger not found")})
    public WorkOrderMeterTrigger getById(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<WorkOrderMeterTrigger> optionalWorkOrderMeterTrigger = workOrderMeterTriggerService.findById(id);
        if (optionalWorkOrderMeterTrigger.isPresent()) {
            WorkOrderMeterTrigger savedWorkOrderMeterTrigger = optionalWorkOrderMeterTrigger.get();
            return savedWorkOrderMeterTrigger;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied")})
    public WorkOrderMeterTrigger create(@ApiParam("WorkOrderMeterTrigger") @Valid @RequestBody WorkOrderMeterTrigger workOrderMeterTriggerReq, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        return workOrderMeterTriggerService.create(workOrderMeterTriggerReq);
    }


    @GetMapping("/meter/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "WorkOrderMeterTrigger not found")})
    public Collection<WorkOrderMeterTriggerShowDTO> getByMeter(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Meter> optionalMeter = meterService.findById(id);
        if (optionalMeter.isPresent()) {
            return workOrderMeterTriggerService.findByMeter(id).stream().map(workOrderMeterTriggerMapper::toShowDto).collect(Collectors.toList());
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }


    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "WorkOrderMeterTrigger not found")})
    public WorkOrderMeterTriggerShowDTO patch(@ApiParam("WorkOrderMeterTrigger") @Valid @RequestBody WorkOrderMeterTriggerPatchDTO workOrderMeterTrigger, @ApiParam("id") @PathVariable("id") Long id,
                                              HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<WorkOrderMeterTrigger> optionalWorkOrderMeterTrigger = workOrderMeterTriggerService.findById(id);

        if (optionalWorkOrderMeterTrigger.isPresent()) {
            WorkOrderMeterTrigger savedWorkOrderMeterTrigger = optionalWorkOrderMeterTrigger.get();
            return workOrderMeterTriggerMapper.toShowDto(workOrderMeterTriggerService.update(id, workOrderMeterTrigger));
        } else throw new CustomException("WorkOrderMeterTrigger not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "WorkOrderMeterTrigger not found")})
    public ResponseEntity delete(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<WorkOrderMeterTrigger> optionalWorkOrderMeterTrigger = workOrderMeterTriggerService.findById(id);
        if (optionalWorkOrderMeterTrigger.isPresent()) {
            WorkOrderMeterTrigger savedWorkOrderMeterTrigger = optionalWorkOrderMeterTrigger.get();
            workOrderMeterTriggerService.delete(id);
            return new ResponseEntity(new SuccessResponse(true, "Deleted successfully"),
                    HttpStatus.OK);
        } else throw new CustomException("WorkOrderMeterTrigger not found", HttpStatus.NOT_FOUND);
    }

}
