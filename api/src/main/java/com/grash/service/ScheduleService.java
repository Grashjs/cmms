package com.grash.service;

import com.grash.dto.SchedulePatchDTO;
import com.grash.exception.CustomException;
import com.grash.job.PreventiveMaintenanceNotificationJob;
import com.grash.job.WorkOrderCreationJob;
import com.grash.mapper.ScheduleMapper;
import com.grash.model.PreventiveMaintenance;
import com.grash.model.Schedule;
import com.grash.model.WorkOrder;
import com.grash.model.enums.Status;
import com.grash.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import com.grash.model.enums.RecurrenceBasedOn;
import org.quartz.CronScheduleBuilder;
import org.quartz.SimpleScheduleBuilder; // Added import for SimpleScheduleBuilder

import java.util.Calendar;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final ScheduleMapper scheduleMapper;
    private final WorkOrderService workOrderService;

    // Quartz Scheduler
    private final Scheduler scheduler;

    public Schedule create(Schedule Schedule) {
        return scheduleRepository.save(Schedule);
    }

    public Schedule update(Long id, SchedulePatchDTO schedule) {
        if (scheduleRepository.existsById(id)) {
            Schedule savedSchedule = scheduleRepository.findById(id).get();
            return scheduleRepository.save(scheduleMapper.updateSchedule(savedSchedule, schedule));
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public Collection<Schedule> getAll() {
        return scheduleRepository.findAll();
    }

    public void delete(Long id) {
        // Ensure jobs are killed before deleting data
        stopScheduleTimers(id);
        scheduleRepository.deleteById(id);
    }

    public Optional<Schedule> findById(Long id) {
        return scheduleRepository.findById(id);
    }

    public Collection<Schedule> findByCompany(Long id) {
        return scheduleRepository.findByCompany_Id(id);
    }

    public void scheduleWorkOrder(Schedule schedule) {
        int limit = 10; //inclusive schedules at 10
        PreventiveMaintenance preventiveMaintenance = schedule.getPreventiveMaintenance();
        Page<WorkOrder> workOrdersPage = workOrderService.findLastByPM(preventiveMaintenance.getId(), limit);

        boolean isStale = false;
        if (workOrdersPage.getTotalElements() >= limit && workOrdersPage.getContent().stream().allMatch(workOrder -> workOrder.getFirstTimeToReact() == null)) {
            isStale = true;
            schedule.setDisabled(true);
            scheduleRepository.save(schedule);
        }

        boolean shouldSchedule =
                !schedule.isDisabled() && (schedule.getEndsOn() == null || schedule.getEndsOn().after(new Date())) && !isStale;

        if (shouldSchedule) {
            try {
                ScheduleBuilder scheduleBuilder = null;
                Date startsOn = schedule.getStartsOn();

                if (schedule.getRecurrenceBasedOn() == RecurrenceBasedOn.COMPLETED_DATE) {
                    if (workOrdersPage.isEmpty()) {
                        scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                                .withRepeatCount(0);
                    } else {
                        WorkOrder lastCompletedWorkOrder =
                                workOrdersPage.stream()
                                        .filter(w -> Status.COMPLETE.equals(w.getStatus()))
                                        .max(Comparator.comparing(WorkOrder::getCompletedOn))
                                        .orElse(null);
                        if (lastCompletedWorkOrder == null) return;
                        scheduleNextWorkOrderJobAfterCompletion(schedule.getId(),
                                lastCompletedWorkOrder.getCompletedOn());
                    }
                } else { // SCHEDULED_DATE
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(startsOn);
                    int hour = cal.get(Calendar.HOUR_OF_DAY);
                    int minute = cal.get(Calendar.MINUTE);

                    switch (schedule.getRecurrenceType()) {
                        case DAILY:
                            scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                                    .withIntervalInHours(24 * schedule.getFrequency())
                                    .repeatForever();
                            break;

                        case WEEKLY:
                            if (schedule.getDaysOfWeek() == null || schedule.getDaysOfWeek().isEmpty()) {
                                throw new CustomException("Days of week are required for weekly recurrence.",
                                        HttpStatus.BAD_REQUEST);
                            }

                            // Convert ISO days to Quartz format
                            String daysOfWeekCron = schedule.getDaysOfWeek().stream()
                                    .map(d -> (d + 1) % 7 + 1) // 0-based (Mon=0) to Calendar (Sun=1, Mon=2)
                                    .map(String::valueOf)
                                    .collect(Collectors.joining(","));

                            String cronExpression = String.format("0 %d %d ? * %s", minute, hour, daysOfWeekCron);
                            scheduleBuilder = CronScheduleBuilder.cronSchedule(cronExpression)
                                    .inTimeZone(TimeZone.getDefault());

                            // Store the frequency in the job data so the job can handle it
                            // The cron will fire every week on specified days, but the job will check frequency
                            break;

                        case MONTHLY:
                            scheduleBuilder = CalendarIntervalScheduleBuilder.calendarIntervalSchedule()
                                    .withIntervalInMonths(schedule.getFrequency())
                                    .preserveHourOfDayAcrossDaylightSavings(true);
                            break;

                        case YEARLY:
                            scheduleBuilder = CalendarIntervalScheduleBuilder.calendarIntervalSchedule()
                                    .withIntervalInYears(schedule.getFrequency())
                                    .preserveHourOfDayAcrossDaylightSavings(true);
                            break;

                        default:
                            throw new CustomException("Unsupported recurrence type: " + schedule.getRecurrenceType(),
                                    HttpStatus.BAD_REQUEST);
                    }
                }

                // ---------------------------------------------------------
                // JOB 1: Work Order Creation
                // ---------------------------------------------------------
                JobDetail woJob = JobBuilder.newJob(WorkOrderCreationJob.class)
                        .withIdentity("wo-job-" + schedule.getId(), "wo-group")
                        .usingJobData("scheduleId", schedule.getId())
                        .storeDurably()
                        .build();

                Trigger woTrigger = TriggerBuilder.newTrigger()
                        .withIdentity("wo-trigger-" + schedule.getId(), "wo-group")
                        .startAt(startsOn)
                        .withSchedule(scheduleBuilder)
                        .endAt(schedule.getEndsOn())
                        .build();

                scheduler.scheduleJob(woJob, woTrigger);

                // ---------------------------------------------------------
                // JOB 2: Notification (Shared Method Call)
                // ---------------------------------------------------------
                int daysBeforePMNotification = preventiveMaintenance.getCompany()
                        .getCompanySettings().getGeneralPreferences().getDaysBeforePrevMaintNotification();

                if (daysBeforePMNotification > 0) {
                    Date trueStartsOnForNotif = preventiveMaintenance.getEstimatedStartDate() == null ? startsOn :
                            preventiveMaintenance.getEstimatedStartDate();

                    // Call the shared method for initial setup
                    scheduleNotificationJob(
                            schedule.getId(),
                            trueStartsOnForNotif,
                            schedule.getEndsOn(),
                            scheduleBuilder, // Pass the recurrence schedule
                            daysBeforePMNotification
                    );
                }

            } catch (SchedulerException e) {
                log.error("Error scheduling quartz job for schedule " + schedule.getId(), e);
                // Depending on your error handling policy, you might want to throw a RuntimeException here
            }
        }
    }

    public void reScheduleWorkOrder(Long id, Schedule schedule) {
        // Quartz "reschedule" is best handled by deleting and recreating
        // to ensure all parameters (trigger times, data map) are fresh.
        stopScheduleTimers(id);
        scheduleWorkOrder(schedule);
    }

    public void stopScheduleTimers(Long id) {
        try {
            // Delete Work Order Job
            scheduler.deleteJob(new JobKey("wo-job-" + id, "wo-group"));

            // Delete Notification Job (it might not exist, but Quartz handles that gracefully usually, or returns
            // false)
            scheduler.deleteJob(new JobKey("notif-job-" + id, "notif-group"));

        } catch (SchedulerException e) {
            log.error("Error stopping quartz jobs for schedule " + id, e);
        }
    }

    // =========================================================================================
    // NEW SHARED NOTIFICATION SCHEDULING METHOD
    // =========================================================================================

    private void scheduleNotificationJob(
            Long scheduleId,
            Date startsOn,
            Date endsOn,
            ScheduleBuilder scheduleBuilder,
            int daysBeforeNotification) throws SchedulerException {

        // 1. Calculate the actual start date for the notification (shifted back)
        Calendar notifCal = Calendar.getInstance();
        notifCal.setTime(startsOn);
        notifCal.add(Calendar.DATE, -daysBeforeNotification);
        Date notificationStart = notifCal.getTime();

        JobDetail notifJob = JobBuilder.newJob(PreventiveMaintenanceNotificationJob.class)
                .withIdentity("notif-job-" + scheduleId, "notif-group")
                .usingJobData("scheduleId", scheduleId)
                .build();

        Trigger notifTrigger = TriggerBuilder.newTrigger()
                .withIdentity("notif-trigger-" + scheduleId, "notif-group")
                .startAt(notificationStart)
                .withSchedule(scheduleBuilder)
                .endAt(endsOn)
                .build();

        scheduler.scheduleJob(notifJob, notifTrigger);
    }

    // =========================================================================================
    // CHAINING METHOD (Updated to use the shared notification method)
    // =========================================================================================

    public void scheduleNextWorkOrderJobAfterCompletion(Long scheduleId, Date completedDate) {
        Optional<Schedule> scheduleOpt = scheduleRepository.findById(scheduleId);
        if (!scheduleOpt.isPresent()) return;

        Schedule schedule = scheduleOpt.get();

        // Only applies to COMPLETED_DATE schedules
        if (schedule.getRecurrenceBasedOn() != RecurrenceBasedOn.COMPLETED_DATE) return;

        // 1. Calculate the next run date based on Frequency
        Calendar cal = Calendar.getInstance();
        cal.setTime(completedDate);

        switch (schedule.getRecurrenceType()) {
            case DAILY:
                cal.add(Calendar.DAY_OF_YEAR, schedule.getFrequency());
                break;
            case WEEKLY:
                cal.add(Calendar.WEEK_OF_YEAR, schedule.getFrequency());
                break;
            case MONTHLY:
                cal.add(Calendar.MONTH, schedule.getFrequency());
                break;
            case YEARLY:
                cal.add(Calendar.YEAR, schedule.getFrequency());
                break;
        }
        Date nextRunDate = cal.getTime();

        // The one-shot schedule for the chained job
        ScheduleBuilder oneShotSchedule = SimpleScheduleBuilder.simpleSchedule().withRepeatCount(0);

        // 2. Schedule a "One-Shot" Job for that date
        try {
            JobKey jobKey = new JobKey("wo-job-chained-" + schedule.getId() + "-" + nextRunDate.getTime(), "wo-group");

            JobDetail woJob = JobBuilder.newJob(WorkOrderCreationJob.class)
                    .withIdentity(jobKey)
                    .usingJobData("scheduleId", schedule.getId())
                    .storeDurably()
                    .build();

            Trigger woTrigger = TriggerBuilder.newTrigger()
                    .startAt(nextRunDate)
                    .withSchedule(oneShotSchedule) // Run Once
                    .build();

            scheduler.scheduleJob(woJob, woTrigger);
            log.info("Chained next schedule for Schedule ID {} at {}", schedule.getId(), nextRunDate);

            PreventiveMaintenance pm = schedule.getPreventiveMaintenance();
            int daysBeforePMNotification = pm.getCompany()
                    .getCompanySettings().getGeneralPreferences().getDaysBeforePrevMaintNotification();

            if (daysBeforePMNotification > 0) {
                scheduleNotificationJob(
                        schedule.getId(),
                        nextRunDate, // The calculated WO start date
                        schedule.getEndsOn(),
                        oneShotSchedule, // The one-shot schedule for notification
                        daysBeforePMNotification
                );
            }

        } catch (SchedulerException e) {
            log.error("Failed to chain next schedule for ID " + schedule.getId(), e);
        }
    }

    public Schedule save(Schedule schedule) {
        return scheduleRepository.saveAndFlush(schedule);
    }
}