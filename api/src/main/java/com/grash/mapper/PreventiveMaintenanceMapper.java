package com.grash.mapper;

import com.grash.dto.*;
import com.grash.model.PreventiveMaintenance;
import com.grash.model.Schedule;
import com.grash.model.WorkOrder;
import com.grash.model.enums.RecurrenceBasedOn;
import com.grash.model.enums.RecurrenceType;
import com.grash.model.enums.Status;
import com.grash.service.WorkOrderService;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Mapper(componentModel = "spring", uses = {LocationMapper.class, TeamMapper.class, UserMapper.class,
        CustomerMapper.class, AssetMapper.class, FileMapper.class})
@Slf4j
public abstract class PreventiveMaintenanceMapper {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private WorkOrderService workOrderService;

    public abstract PreventiveMaintenance updatePreventiveMaintenance(@MappingTarget PreventiveMaintenance entity,
                                                                      PreventiveMaintenancePatchDTO dto);

    @Mappings({})
    public abstract PreventiveMaintenancePatchDTO toPatchDto(PreventiveMaintenance model);

    public abstract PreventiveMaintenanceShowDTO toShowDto(PreventiveMaintenance model);

    public abstract PreventiveMaintenance toModel(PreventiveMaintenancePostDTO dto);

    public abstract PreventiveMaintenanceMiniDTO toMiniDto(PreventiveMaintenance model);

    public abstract WorkOrderBaseMiniDTO toBaseMiniDto(PreventiveMaintenance model);

    @AfterMapping
    protected void addNextWorkOrderDate(@MappingTarget PreventiveMaintenanceShowDTO dto, PreventiveMaintenance pm) {
        Schedule schedule = pm.getSchedule();
        if (schedule == null || schedule.isDisabled()) {
            return;
        }

        Date nextWorkOrderDate = null;

        if (schedule.getRecurrenceBasedOn() == RecurrenceBasedOn.SCHEDULED_DATE) {
            try {
                TriggerKey triggerKey = new TriggerKey("wo-trigger-" + schedule.getId(), "wo-group");
                Trigger trigger = scheduler.getTrigger(triggerKey);
                if (trigger != null) {
                    // Special handling for WEEKLY with frequency > 1
                    if (schedule.getRecurrenceType() == RecurrenceType.WEEKLY && schedule.getFrequency() > 1) {
                        nextWorkOrderDate = calculateNextWeeklyWorkOrderDate(schedule);
                    } else {
                        nextWorkOrderDate = trigger.getNextFireTime();
                    }
                }
            } catch (SchedulerException e) {
                log.error("Error getting next fire time for schedule " + schedule.getId(), e);
            }
        } else if (schedule.getRecurrenceBasedOn() == RecurrenceBasedOn.COMPLETED_DATE) {
            Page<WorkOrder> workOrdersPage = workOrderService.findLastByPM(pm.getId(), 1);

            if (!workOrdersPage.hasContent()) {
                nextWorkOrderDate = schedule.getStartsOn();
            } else {
                WorkOrder lastWorkOrder = workOrdersPage.getContent().get(0);
                if (lastWorkOrder.getStatus() != null && lastWorkOrder.getStatus().equals(Status.COMPLETE) && lastWorkOrder.getCompletedOn() != null) {
                    Date completedDate = lastWorkOrder.getCompletedOn();
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
                    nextWorkOrderDate = cal.getTime();
                }
            }
        }

        dto.setNextWorkOrderDate(nextWorkOrderDate);
    }

    private Date calculateNextWeeklyWorkOrderDate(Schedule schedule) {
        Date now = new Date();
        Date startsOn = schedule.getStartsOn();

        // Calculate weeks since start
        long daysSinceStart = ChronoUnit.DAYS.between(
                startsOn.toInstant(),
                now.toInstant()
        );
        long weeksSinceStart = daysSinceStart / 7;

        // Find the next valid week
        long nextValidWeek = ((weeksSinceStart / schedule.getFrequency()) + 1) * schedule.getFrequency();

        // Calculate the date of that week
        Calendar cal = Calendar.getInstance();
        cal.setTime(startsOn);
        cal.add(Calendar.WEEK_OF_YEAR, (int) nextValidWeek);

        // Find the first matching day of week from that week onwards
        List<Integer> daysOfWeek = schedule.getDaysOfWeek();
        if (daysOfWeek == null || daysOfWeek.isEmpty()) {
            return cal.getTime();
        }

        // Get current day of week
        int currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

        // Convert ISO days to Calendar days and find the earliest one
        int earliestDay = daysOfWeek.stream()
                .map(d -> (d + 1) % 7 + 1) // 0-based (Mon=0) to Calendar (Sun=1, Mon=2)
                .min(Integer::compare)
                .orElse(currentDayOfWeek);

        // Set to that day
        cal.set(Calendar.DAY_OF_WEEK, earliestDay);

        return cal.getTime();
    }
}
