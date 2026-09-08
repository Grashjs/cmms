package com.grash.service;

import com.grash.dto.DateRange;
import com.grash.dto.ReadingHistogramDTO;
import com.grash.dto.ReadingPatchDTO;
import com.grash.dto.workOrder.WorkOrderPostDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.ReadingMapper;
import com.grash.mapper.WorkOrderMapper;
import com.grash.model.Meter;
import com.grash.model.Notification;
import com.grash.model.Reading;
import com.grash.model.User;
import com.grash.model.WorkOrder;
import com.grash.model.WorkOrderMeterTrigger;
import com.grash.model.enums.NotificationType;
import com.grash.model.enums.PlanFeatures;
import com.grash.model.enums.WorkOrderMeterTriggerCondition;
import com.grash.model.enums.webhook.WebhookEvent;
import com.grash.repository.ReadingRepository;
import com.grash.utils.Helper;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReadingService {
    private final ReadingRepository readingRepository;
    private final ReadingMapper readingMapper;
    private final LicenseService licenseService;
    private final WorkOrderMeterTriggerService workOrderMeterTriggerService;
    private final NotificationService notificationService;
    private final WorkOrderService workOrderService;
    private final MessageSource messageSource;
    private final WebhookDispatchService webhookDispatchService;
    private final WorkOrderMapper workOrderMapper;
    private MeterService meterService;

    @Autowired
    public void setDeps(@Lazy MeterService meterService
    ) {
        this.meterService = meterService;
    }

    @Transactional
    public Reading create(Reading readingReq, User user) {
        if (!user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.METER))
            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        Optional<Meter> optionalMeter = meterService.findById(readingReq.getMeter().getId());
        if (optionalMeter.isPresent()) {
            Meter meter = optionalMeter.get();
            if (!meter.canBeViewedBy(user))
                throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            Optional<Reading> optionalLastReading = findLastByMeter(readingReq.getMeter().getId());
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
            return readingRepository.save(readingReq);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public Collection<Reading> getAll() {
        return readingRepository.findAll();
    }

    public Collection<Reading> getByMeter(Long id, User user) {
        Optional<Meter> optionalMeter = meterService.findById(id);
        if (optionalMeter.isPresent()) {
            if (!optionalMeter.get().canBeViewedBy(user))
                throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            return readingRepository.findByMeter_Id(id);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public List<ReadingHistogramDTO> getHistogram(Long id, DateRange dateRange, User user) {
        Optional<Meter> optionalMeter = meterService.findById(id);
        if (optionalMeter.isEmpty()) {
            throw new CustomException("Meter not found", HttpStatus.NOT_FOUND);
        }
        if (!optionalMeter.get().canBeViewedBy(user))
            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        if (dateRange.getStart() == null || dateRange.getEnd() == null) {
            throw new CustomException("Start and end dates are required", HttpStatus.BAD_REQUEST);
        }
        if (dateRange.getStart().after(dateRange.getEnd())) {
            throw new CustomException("Start date must be before end date", HttpStatus.BAD_REQUEST);
        }
        return getHistogramData(id, dateRange.getStart(),
                dateRange.getEnd(), user.getCompany().getCompanySettings().getGeneralPreferences().getTimeZone());
    }

    @Transactional
    public Reading patch(Long id, ReadingPatchDTO reading, User user) {
        Optional<Reading> optionalReading = readingRepository.findById(id);

        if (optionalReading.isPresent()) {
            Reading savedReading = optionalReading.get();
            if (!savedReading.getMeter().canBeViewedBy(user))
                throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            Reading updated = readingRepository.save(readingMapper.updateReading(savedReading, reading));
            processMeterTriggers(savedReading.getMeter(), updated.getValue(), user);
            return updated;
        } else throw new CustomException("Reading not found", HttpStatus.NOT_FOUND);
    }

    @Transactional
    public void deleteByIdAndUser(Long id, User user) {
        Optional<Reading> optionalReading = readingRepository.findById(id);

        if (optionalReading.isPresent()) {
            if (!optionalReading.get().getMeter().canBeViewedBy(user))
                throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            readingRepository.deleteById(id);
        } else throw new CustomException("Reading not found", HttpStatus.NOT_FOUND);
    }

    public Optional<Reading> findById(Long id) {
        return readingRepository.findById(id);
    }

    public Collection<Reading> findByCompany(Long id) {
        return readingRepository.findByCompany_Id(id);
    }

    public Optional<Reading> findLastByMeter(Long id) {
        return readingRepository.findFirstByMeter_IdOrderByCreatedAtDesc(id);
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

    public List<ReadingHistogramDTO> getHistogramData(Long meterId, Date start, Date end, @NotNull String timeZone) {
        Collection<Reading> readings = readingRepository.findByMeter_IdAndCreatedAtBetween(meterId, start, end);
        if (readings.isEmpty()) {
            return Collections.emptyList();
        }

        long totalDays = TimeUnit.MILLISECONDS.toDays(end.getTime() - start.getTime()) + 1;
        int maxPoints = 30;

        List<Reading> sorted = readings.stream()
                .sorted(Comparator.comparing(Reading::getCreatedAt))
                .toList();

        int bucketSize = (int) Math.max(1, Math.ceil((double) totalDays / maxPoints));

        Calendar cal = Calendar.getInstance();
        cal.setTime(start);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.setTimeZone(TimeZone.getTimeZone(timeZone));

        List<ReadingHistogramDTO> result = new ArrayList<>();
        Date bucketStart = cal.getTime();

        while (!bucketStart.after(end)) {
            cal.setTime(bucketStart);
            cal.add(Calendar.DAY_OF_MONTH, bucketSize);
            cal.add(Calendar.MILLISECOND, -1);
            Date bucketEnd = cal.getTime();
            if (bucketEnd.after(end)) {
                bucketEnd = end;
            }

            final Date bStart = bucketStart;
            final Date bEnd = bucketEnd;
            List<Reading> bucket = sorted.stream()
                    .filter(r -> !r.getCreatedAt().before(bStart) && !r.getCreatedAt().after(bEnd))
                    .toList();

            if (!bucket.isEmpty()) {
                double avg = bucket.stream().mapToDouble(Reading::getValue).average().orElse(0);
                Date midpoint = new Date((bStart.getTime() + bEnd.getTime()) / 2);
                result.add(ReadingHistogramDTO.builder()
                        .date(midpoint)
                        .value(Math.round(avg * 100.0) / 100.0)
                        .count(bucket.size())
                        .build());
            }

            cal.setTime(bucketStart);
            cal.add(Calendar.DAY_OF_MONTH, bucketSize);
            bucketStart = cal.getTime();
        }

        return result;
    }
}