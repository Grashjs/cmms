package com.grash.controller;

import com.grash.dto.DateRange;
import com.grash.dto.ReadingHistogramDTO;
import com.grash.dto.ReadingPatchDTO;
import com.grash.dto.SuccessResponse;
import com.grash.dto.workOrder.WorkOrderPostDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.WorkOrderMapper;
import com.grash.model.*;
import com.grash.model.enums.NotificationType;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PlanFeatures;
import com.grash.model.enums.WorkOrderMeterTriggerCondition;
import com.grash.model.enums.webhook.WebhookEvent;
import com.grash.service.*;
import com.grash.utils.AuditComparator;
import com.grash.utils.Helper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/readings")
@Tag(name = "Readings", description = "Operations on meter readings")
@RequiredArgsConstructor
public class ReadingController {

    private final MeterService meterService;
    private final ReadingService readingService;
    private final UserService userService;
    private final WorkOrderMeterTriggerService workOrderMeterTriggerService;
    private final NotificationService notificationService;
    private final WorkOrderService workOrderService;
    private final MessageSource messageSource;
    private final WebhookDispatchService webhookDispatchService;
    private final WorkOrderMapper workOrderMapper;


    @GetMapping("/meter/{id}")
    @PreAuthorize("permitAll()")
    public Collection<Reading> getByMeter(@PathVariable("id") Long id, HttpServletRequest req) {
        User user = userService.whoami(req);
        Optional<Meter> optionalMeter = meterService.findById(id);
        if (optionalMeter.isPresent()) {
            if (!meterService.isAccessibleBy(user, optionalMeter.get()))
                throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            return readingService.findByMeter(id);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PostMapping("/meter/{id}/histogram")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Get histogram data for a meter within a date range (max 30 points)")
    public List<ReadingHistogramDTO> getHistogram(
            @PathVariable("id") Long id,
            @RequestBody DateRange dateRange,
            HttpServletRequest req) {
        User user = userService.whoami(req);
        Optional<Meter> optionalMeter = meterService.findById(id);
        if (optionalMeter.isEmpty()) {
            throw new CustomException("Meter not found", HttpStatus.NOT_FOUND);
        }
        if (!meterService.isAccessibleBy(user, optionalMeter.get()))
            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        if (dateRange.getStart() == null || dateRange.getEnd() == null) {
            throw new CustomException("Start and end dates are required", HttpStatus.BAD_REQUEST);
        }
        if (dateRange.getStart().after(dateRange.getEnd())) {
            throw new CustomException("Start date must be before end date", HttpStatus.BAD_REQUEST);
        }
        return readingService.getHistogramData(id, dateRange.getStart(),
                dateRange.getEnd());
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    Reading create(@Parameter(description = "Reading data to create") @Valid @RequestBody Reading readingReq,
                   HttpServletRequest req) {
        User user = userService.whoami(req);
        if (!user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.METER))
            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        Optional<Meter> optionalMeter = meterService.findById(readingReq.getMeter().getId());
        if (optionalMeter.isPresent()) {
            Meter meter = optionalMeter.get();
            if (!meterService.isAccessibleBy(user, meter))
                throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            Optional<Reading> optionalLastReading = readingService.findLastByMeter(readingReq.getMeter().getId());
            if (optionalLastReading.isPresent()) {
                Reading lastReading = optionalLastReading.get();
                String timeZone = meter.getCompany()
                        .getCompanySettings()
                        .getGeneralPreferences()
                        .getTimeZone();
                LocalDate nextReading =
                        Helper.dateToLocalDate(lastReading.getCreatedAt()).plusDays(meter.getUpdateFrequency());
                if (LocalDate.now(ZoneId.of(timeZone)).isBefore(nextReading)) {
                    throw new CustomException("The update frequency has not been respected", HttpStatus.NOT_ACCEPTABLE);
                }
            }
            processMeterTriggers(meter, readingReq.getValue(), user);
            return readingService.create(readingReq);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public Reading patch(@Parameter(description = "Reading fields to update") @Valid @RequestBody ReadingPatchDTO reading,
                         @PathVariable("id") Long id,
                         HttpServletRequest req) {
        User user = userService.whoami(req);
        Optional<Reading> optionalReading = readingService.findById(id);

        if (optionalReading.isPresent()) {
            Reading savedReading = optionalReading.get();
            if (!meterService.isAccessibleBy(user, savedReading.getMeter()))
                throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            Reading updated = readingService.update(id, reading);
            processMeterTriggers(savedReading.getMeter(), updated.getValue(), user);
            return updated;
        } else throw new CustomException("Reading not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> delete(@PathVariable("id") Long id, HttpServletRequest req) {
        User user = userService.whoami(req);

        Optional<Reading> optionalReading = readingService.findById(id);
        if (optionalReading.isPresent()) {
            if (!meterService.isAccessibleBy(user, optionalReading.get().getMeter()))
                throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            readingService.delete(id);
            return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"),
                    HttpStatus.OK);
        } else throw new CustomException("Reading not found", HttpStatus.NOT_FOUND);
    }

    private void processMeterTriggers(Meter meter, double readingValue, User user) {
        Collection<WorkOrderMeterTrigger> meterTriggers = workOrderMeterTriggerService.findByMeter(meter.getId());
        Locale locale = Helper.getLocale(user);
        meterTriggers.forEach(meterTrigger -> {
            boolean error = false;
            StringBuilder message = new StringBuilder();
            String title = messageSource.getMessage("new_wo", null, locale);
            Object[] notificationArgs = new Object[]{meter.getName(), meterTrigger.getValue(), meter.getUnit()};
            if (meterTrigger.getTriggerCondition().equals(WorkOrderMeterTriggerCondition.LESS_THAN)) {
                if (readingValue < meterTrigger.getValue()) {
                    error = true;
                    message.append(messageSource.getMessage("notification_reading_less_than", notificationArgs,
                            locale));
                }
            } else if (readingValue > meterTrigger.getValue()) {
                error = true;
                message.append(messageSource.getMessage("notification_reading_more_than", notificationArgs,
                        locale));
            }
            if (error) {
                notificationService.createMultiple(meter.getUsers().stream().map(user1 ->
                        new Notification(message.toString(), user1, NotificationType.METER, meter.getId())
                ).collect(Collectors.toList()), true, title);
                WorkOrderPostDTO workOrder = workOrderService.getWorkOrderFromWorkOrderBase(meterTrigger);
                WorkOrder createdWorkOrder = workOrderService.create(workOrder, user.getCompany());

                Map<String, Object> webhookPayload = new HashMap<>();
                webhookPayload.put("meterId", meter.getId());
                webhookPayload.put("meterName", meter.getName());
                webhookPayload.put("meterTriggerId", meterTrigger.getId());
                webhookPayload.put("meterTriggerName", meterTrigger.getName());
                webhookPayload.put("readingValue", readingValue);
                webhookPayload.put("triggerValue", meterTrigger.getValue());
                webhookPayload.put("triggerCondition", meterTrigger.getTriggerCondition().name());
                webhookPayload.put("workOrderId", createdWorkOrder.getId());
                Object serializedWorkOrder = workOrderMapper.toShowDto(createdWorkOrder);
                webhookDispatchService.dispatchWebhook(user.getCompany(),
                        WebhookEvent.METER_TRIGGER_STATUS_CHANGE, webhookPayload,
                        "triggeredWorkOrder", serializedWorkOrder, null, null, null, null, null);
            }
        });
    }

}


