package com.grash.controller;

import com.grash.dto.ReadingPatchDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.enums.NotificationType;
import com.grash.model.enums.WorkOrderMeterTriggerCondition;
import com.grash.service.*;
import com.grash.utils.AuditComparator;
import com.grash.utils.Helper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/readings")
@Api(tags = "reading")
@RequiredArgsConstructor
public class ReadingController {

    private final MeterService meterService;
    private final ReadingService readingService;
    private final UserService userService;
    private final WorkOrderMeterTriggerService workOrderMeterTriggerService;
    private final NotificationService notificationService;
    private final WorkOrderService workOrderService;
    private final MessageSource messageSource;


    @GetMapping("/meter/{id}")
    @PreAuthorize("permitAll()")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Reading not found")})
    public Collection<Reading> getByMeter(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Meter> optionalMeter = meterService.findById(id);
        if (optionalMeter.isPresent()) {
            return readingService.findByMeter(id);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied")})
    public Reading create(@ApiParam("Reading") @Valid @RequestBody Reading readingReq, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Meter> optionalMeter = meterService.findById(readingReq.getMeter().getId());
        if (optionalMeter.isPresent()) {
            Meter meter = optionalMeter.get();
            Collection<Reading> readings = readingService.findByMeter(readingReq.getMeter().getId());
            if (!readings.isEmpty()) {
                Reading lastReading = Collections.max(readings, new AuditComparator());
                Date nextReading = Helper.incrementDays(lastReading.getCreatedAt(), meter.getUpdateFrequency());
                if (new Date().before(nextReading)) {
                    throw new CustomException("The update frequency has not been respected", HttpStatus.NOT_ACCEPTABLE);
                }
            }
            Collection<WorkOrderMeterTrigger> meterTriggers = workOrderMeterTriggerService.findByMeter(meter.getId());
            Locale locale = Helper.getLocale(user);
            meterTriggers.forEach(meterTrigger -> {
                boolean error = false;
                StringBuilder message = new StringBuilder();
                String title = messageSource.getMessage("new_wo", null, locale);
                Object[] notificationArgs = new Object[]{meter.getName(), meterTrigger.getValue(), meter.getUnit()};
                if (meterTrigger.getTriggerCondition().equals(WorkOrderMeterTriggerCondition.LESS_THAN)) {
                    if (readingReq.getValue() < meterTrigger.getValue()) {
                        error = true;
                        message.append(messageSource.getMessage("notification_reading_less_than", notificationArgs,
                                locale));
                    }
                } else if (readingReq.getValue() > meterTrigger.getValue()) {
                    error = true;
                    message.append(messageSource.getMessage("notification_reading_more_than", notificationArgs,
                            locale));
                }
                if (error) {
                    notificationService.createMultiple(meter.getUsers().stream().map(user1 ->
                            new Notification(message.toString(), user1, NotificationType.METER, meter.getId())
                    ).collect(Collectors.toList()), true, title);
                    WorkOrder workOrder = workOrderService.getWorkOrderFromWorkOrderBase(meterTrigger);
                    workOrderService.create(workOrder, user.getCompany());
                }
            });
            return readingService.create(readingReq);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "Reading not found")})
    public Reading patch(@ApiParam("Reading") @Valid @RequestBody ReadingPatchDTO reading,
                         @ApiParam("id") @PathVariable("id") Long id,
                         HttpServletRequest req) {
        OwnUser user = userService.whoami(req);
        Optional<Reading> optionalReading = readingService.findById(id);

        if (optionalReading.isPresent()) {
            Reading savedReading = optionalReading.get();
            return readingService.update(id, reading);
        } else throw new CustomException("Reading not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @ApiResponses(value = {//
            @ApiResponse(code = 500, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "Reading not found")})
    public ResponseEntity<SuccessResponse> delete(@ApiParam("id") @PathVariable("id") Long id, HttpServletRequest req) {
        OwnUser user = userService.whoami(req);

        Optional<Reading> optionalReading = readingService.findById(id);
        if (optionalReading.isPresent()) {
            readingService.delete(id);
            return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"),
                    HttpStatus.OK);
        } else throw new CustomException("Reading not found", HttpStatus.NOT_FOUND);
    }
}
