package com.grash.job;

import com.grash.model.PreventiveMaintenance;
import com.grash.model.Schedule;
import com.grash.model.WorkOrder;
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
        if (schedule == null) {
            log.warn("Skipping work order creation, schedule {} no longer exists.", scheduleId);
            return;
        }
        if (schedule.isDisabled()) {
            log.info("Skipping work order creation, schedule {} is disabled.", scheduleId);
            return;
        }
        if (!scheduleService.checkIfWeeklyShouldRun(schedule)) {
            log.info("Skipping work order creation, schedule {} is not on a valid week interval.", scheduleId);
            return;
        }

        PreventiveMaintenance preventiveMaintenance = schedule.getPreventiveMaintenance();
        Long pmId = preventiveMaintenance.getId();
        try {
            WorkOrder workOrder = preventiveMaintenanceService
                    .createWorkOrderFromPreventiveMaintenance(preventiveMaintenance);
            log.info("Work order {} generated for preventive maintenance {} (schedule {}).",
                    workOrder == null ? null : workOrder.getCustomId(), pmId, scheduleId);
        } catch (RuntimeException e) {
            log.error("Failed to generate a work order for preventive maintenance " + pmId
                    + " (schedule " + scheduleId + "), the transaction was rolled back.", e);
            throw e;
        }
    }
}
