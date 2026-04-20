package com.grash.job;

import com.grash.model.PreventiveMaintenance;
import com.grash.model.Schedule;
import com.grash.model.Task;
import com.grash.model.WorkOrder;
import com.grash.model.enums.RecurrenceType;
import com.grash.repository.ScheduleRepository;
import com.grash.service.ScheduleService;
import com.grash.service.TaskService;
import com.grash.service.WorkOrderService;
import com.grash.utils.Helper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkOrderCreationJob extends QuartzJobBean {

    private final ScheduleRepository scheduleRepository;
    private final WorkOrderService workOrderService;
    private final TaskService taskService;
    private final ScheduleService scheduleService;

    @Override
    @Transactional
    public void executeInternal(JobExecutionContext context) throws JobExecutionException {
        Long scheduleId = context.getMergedJobDataMap().getLong("scheduleId");

        // We fetch fresh data from DB to ensure validity
        Schedule schedule = scheduleRepository.findById(scheduleId).orElse(null);
        if (schedule == null || schedule.isDisabled()) {
            return;
        }
        scheduleService.checkIfWeeklyShouldRun(schedule);
        
        PreventiveMaintenance preventiveMaintenance = schedule.getPreventiveMaintenance();

        WorkOrder workOrder = workOrderService.getWorkOrderFromWorkOrderBase(preventiveMaintenance);
        Collection<Task> tasks = taskService.findByPreventiveMaintenance(preventiveMaintenance.getId());
        workOrder.setParentPreventiveMaintenance(preventiveMaintenance);

        if (schedule.getDueDateDelay() != null) {
            workOrder.setDueDate(Helper.incrementDays(new Date(), schedule.getDueDateDelay()));
        }

        WorkOrder savedWorkOrder = workOrderService.create(workOrder, preventiveMaintenance.getCompany());

        tasks.forEach(task -> {
            Task copiedTask = new Task(task.getTaskBase(), savedWorkOrder, null, task.getValue());
            copiedTask.setCompany(preventiveMaintenance.getCompany());
            taskService.create(copiedTask);
        });

//        log.info("Generated Work Order for Schedule ID: {}", scheduleId);
    }
}