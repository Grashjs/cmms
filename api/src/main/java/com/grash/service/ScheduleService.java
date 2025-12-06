package com.grash.service;

import com.grash.dto.SchedulePatchDTO;
import com.grash.exception.CustomException;
import com.grash.job.PreventiveMaintenanceNotificationJob;
import com.grash.job.WorkOrderCreationJob;
import com.grash.mapper.ScheduleMapper;
import com.grash.model.PreventiveMaintenance;
import com.grash.model.Schedule;
import com.grash.model.WorkOrder;
import com.grash.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Date;
import java.util.Optional;

import com.grash.model.enums.RecurrenceBasedOn;
import org.quartz.CronScheduleBuilder;

import java.util.Calendar;
import java.util.TimeZone;
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
                ScheduleBuilder scheduleBuilder;
                Date startsOn = schedule.getStartsOn();

                if (schedule.getRecurrenceBasedOn() == RecurrenceBasedOn.COMPLETED_DATE) {
                    scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                            .withRepeatCount(0);
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
                            // This will trigger every week on the specified days.
                            // The job must handle frequency > 1 week internally.
                            String daysOfWeekCron = schedule.getDaysOfWeek().stream()
                                    .map(d -> (d % 7) + 1) // Convert ISO day (Mon=1..Sun=7) to Quartz (Sun=1..Sat=7)
                                    .map(String::valueOf)
                                    .collect(Collectors.joining(","));

                            String cronExpression = String.format("0 %d %d ? * %s", minute, hour, daysOfWeekCron);
                            scheduleBuilder = CronScheduleBuilder.cronSchedule(cronExpression)
                                    .inTimeZone(TimeZone.getDefault()); // Consider using company-specific timezone
                            break;
                        case MONTHLY:
                            int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
                            // This triggers every 'frequency' months on the given day of the month.
                            String cronMonthly = String.format("0 %d %d %d 1/%d ?", minute, hour, dayOfMonth,
                                    schedule.getFrequency());
                            scheduleBuilder = CronScheduleBuilder.cronSchedule(cronMonthly)
                                    .inTimeZone(TimeZone.getDefault());
                            break;
                        case YEARLY:
                            // Cron doesn't directly support "every N years".
                            // This trigger will fire every year. The job must handle the frequency logic.
                            int dayOfMonthYearly = cal.get(Calendar.DAY_OF_MONTH);
                            int month = cal.get(Calendar.MONTH) + 1; // Calendar month is 0-based
                            String cronYearly = String.format("0 %d %d %d %d ?", minute, hour, dayOfMonthYearly, month);
                            scheduleBuilder = CronScheduleBuilder.cronSchedule(cronYearly)
                                    .inTimeZone(TimeZone.getDefault());
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
                // JOB 2: Notification (Optional based on daysBeforePMNotification)
                // ---------------------------------------------------------
                int daysBeforePMNotification = preventiveMaintenance.getCompany()
                        .getCompanySettings().getGeneralPreferences().getDaysBeforePrevMaintNotification();

                if (daysBeforePMNotification > 0) {
                    Date trueStartsOnForNotif = preventiveMaintenance.getEstimatedStartDate() == null ? startsOn :
                            preventiveMaintenance.getEstimatedStartDate();

                    Calendar notifCal = Calendar.getInstance();
                    notifCal.setTime(trueStartsOnForNotif);
                    notifCal.add(Calendar.DATE, -daysBeforePMNotification);
                    Date notificationStart = notifCal.getTime();

                    JobDetail notifJob = JobBuilder.newJob(PreventiveMaintenanceNotificationJob.class)
                            .withIdentity("notif-job-" + schedule.getId(), "notif-group")
                            .usingJobData("scheduleId", schedule.getId())
                            .build();

                    Trigger notifTrigger = TriggerBuilder.newTrigger()
                            .withIdentity("notif-trigger-" + schedule.getId(), "notif-group")
                            .startAt(notificationStart)
                            .withSchedule(scheduleBuilder) // Uses the same recurrence schedule
                            .endAt(schedule.getEndsOn())
                            .build();

                    scheduler.scheduleJob(notifJob, notifTrigger);
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
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule().withRepeatCount(0)) // Run Once
                    .build();

            scheduler.scheduleJob(woJob, woTrigger);
            log.info("Chained next schedule for Schedule ID {} at {}", schedule.getId(), nextRunDate);

            // (Optional) You can also schedule the Notification Job here similarly
            // by subtracting daysBeforePMNotification from nextRunDate

        } catch (SchedulerException e) {
            log.error("Failed to chain next schedule for ID " + schedule.getId(), e);
        }
    }

    public Schedule save(Schedule schedule) {
        return scheduleRepository.saveAndFlush(schedule);
    }
}