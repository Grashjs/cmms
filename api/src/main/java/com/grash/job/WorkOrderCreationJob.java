package com.grash.job;

import com.grash.model.PreventiveMaintenance;
import com.grash.model.Schedule;
import com.grash.repository.ScheduleRepository;
import com.grash.service.PreventiveMaintenanceService;
import com.grash.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkOrderCreationJob extends QuartzJobBean {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleService scheduleService;
    private final PreventiveMaintenanceService preventiveMaintenanceService;

    @Override
    @Transactional
    public void executeInternal(JobExecutionContext context) throws JobExecutionException {
        Long scheduleId = context.getMergedJobDataMap().getLong("scheduleId");

        Schedule schedule = scheduleRepository.findById(scheduleId).orElse(null);
        if (schedule == null || schedule.isDisabled()) {
            return;
        }
        if (!scheduleService.checkIfWeeklyShouldRun(schedule)) {
            return;
        }

        PreventiveMaintenance preventiveMaintenance = schedule.getPreventiveMaintenance();
        preventiveMaintenanceService.createWorkOrderFromPreventiveMaintenance(preventiveMaintenance);
    }
}
