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
import com.grash.utils.Helper;
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
                // 1. Calculate Frequency in Milliseconds
                long frequencyInMillis = (long) schedule.getFrequency() * 24 * 60 * 60 * 1000;

                // ---------------------------------------------------------
                // JOB 1: Work Order Creation
                // ---------------------------------------------------------
                Date startsOn = Helper.getNextOccurrence(schedule.getStartsOn(), schedule.getFrequency());

                JobDetail woJob = JobBuilder.newJob(WorkOrderCreationJob.class)
                        .withIdentity("wo-job-" + schedule.getId(), "wo-group")
                        .usingJobData("scheduleId", schedule.getId())
                        .storeDurably() // Allows job to exist even without triggers (optional safety)
                        .build();

                Trigger woTrigger = TriggerBuilder.newTrigger()
                        .withIdentity("wo-trigger-" + schedule.getId(), "wo-group")
                        .startAt(startsOn)
                        .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                .withIntervalInMilliseconds(frequencyInMillis)
                                .repeatForever())
                        .endAt(schedule.getEndsOn()) // Native Quartz support for 'endsOn'
                        .build();

                // If a job with this key exists, it is replaced
                scheduler.scheduleJob(woJob, woTrigger);


                // ---------------------------------------------------------
                // JOB 2: Notification (Optional based on daysBeforePMNotification)
                // ---------------------------------------------------------
                int daysBeforePMNotification = preventiveMaintenance.getCompany()
                        .getCompanySettings().getGeneralPreferences().getDaysBeforePrevMaintNotification();

                if (daysBeforePMNotification > 0) {
                    Date trueStartsOn = preventiveMaintenance.getEstimatedStartDate() == null ? startsOn :
                            preventiveMaintenance.getEstimatedStartDate();

                    // Logic preserved: Helper.minusDays, then Helper.getNextOccurrence with frequency 1 (to ensure
                    // it's in future)
                    Date notificationStart = Helper.getNextOccurrence(
                            Helper.minusDays(trueStartsOn, daysBeforePMNotification),
                            1
                    );

                    JobDetail notifJob = JobBuilder.newJob(PreventiveMaintenanceNotificationJob.class)
                            .withIdentity("notif-job-" + schedule.getId(), "notif-group")
                            .usingJobData("scheduleId", schedule.getId())
                            .build();

                    Trigger notifTrigger = TriggerBuilder.newTrigger()
                            .withIdentity("notif-trigger-" + schedule.getId(), "notif-group")
                            .startAt(notificationStart)
                            .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                    .withIntervalInMilliseconds(frequencyInMillis)
                                    .repeatForever())
                            .endAt(schedule.getEndsOn()) // Stop sending emails when schedule ends
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

    public Schedule save(Schedule schedule) {
        return scheduleRepository.saveAndFlush(schedule);
    }
}