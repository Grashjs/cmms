package com.grash.service;

import com.grash.dto.SchedulePatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.ScheduleMapper;
import com.grash.model.*;
import com.grash.model.enums.PermissionEntity;
import com.grash.repository.ScheduleRepository;
import com.grash.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final PreventiveMaintenanceService preventiveMaintenanceService;
    private final ScheduleMapper scheduleMapper;
    private final MessageSource messageSource;
    private final EmailService2 emailService2;
    private final WorkOrderService workOrderService;
    private final TaskService taskService;
    private final UserService userService;
    @Value("${frontend.url}")
    private String frontendUrl;
    private final Map<Long, Map<String, Timer>> timersState = new HashMap<>();

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
        boolean shouldSchedule = !schedule.isDisabled() && (schedule.getEndsOn() == null || schedule.getEndsOn()
                .after(new Date())) && !isStale;
        if (shouldSchedule) {
            Timer timer = new Timer();
            //  Collection<WorkOrder> workOrders = workOrderService.findByPM(schedule.getPreventiveMaintenance()
            //  .getId());
            Date startsOn = Helper.getNextOccurence(schedule.getStartsOn(), schedule.getFrequency());
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {//create WO
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
                }
            };
            timer.scheduleAtFixedRate(timerTask, startsOn, (long) schedule.getFrequency() * 24 * 60 * 60 * 1000);
            Map<String, Timer> localTimers = new HashMap<>();
            localTimers.put("wo_creation", timer);//first wo creation

            Timer timer1 = new Timer();// use daysBeforePrevMaintNotification
            int daysBeforePMNotification = preventiveMaintenance.getCompany()
                    .getCompanySettings().getGeneralPreferences().getDaysBeforePrevMaintNotification();
            if (daysBeforePMNotification > 0) {
                Date trueStartsOn = preventiveMaintenance.getEstimatedStartDate() == null ? startsOn :
                        preventiveMaintenance.getEstimatedStartDate();
                TimerTask timerTask1 = new TimerTask() {
                    @Override
                    public void run() {
                        //send notification to assigned users
                        PreventiveMaintenance preventiveMaintenance = schedule.getPreventiveMaintenance();
                        Locale locale = Helper.getLocale(preventiveMaintenance.getCompany());
                        String title = messageSource.getMessage("coming_wo", null, locale);
                        Collection<OwnUser> admins =
                                userService.findWorkersByCompany(preventiveMaintenance.getCompany().getId()).stream().filter(ownUser -> ownUser.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS)).collect(Collectors.toList());
                        List<OwnUser> usersToMail = new ArrayList<>(Stream.concat(
                                        preventiveMaintenance.getUsers().stream(),
                                        admins.stream()).filter(user -> user.isEnabled() && user.getUserSettings().shouldEmailUpdatesForWorkOrders())
                                .collect(Collectors.toMap(
                                        OwnUser::getId,  // key by ID
                                        Function.identity(), // value is the user object
                                        (existing, replacement) -> existing))  // if duplicate keys, keep existing
                                .values());
                        Map<String, Object> mailVariables = new HashMap<String, Object>() {{
                            put("pmLink",
                                    frontendUrl + "/app/preventive-maintenances/" + preventiveMaintenance.getId());
                            put("featuresLink", frontendUrl + "/#key-features");
                            put("pmTitle", preventiveMaintenance.getTitle());
                        }};
                        emailService2.sendMessageUsingThymeleafTemplate(usersToMail.stream().map(OwnUser::getEmail)
                                .toArray(String[]::new), title, mailVariables, "coming-work-order.html", locale);
                    }
                };
                timer1.scheduleAtFixedRate(timerTask1, Helper.getNextOccurence(Helper.minusDays(trueStartsOn,
                                daysBeforePMNotification), 1),
                        (long) schedule.getFrequency() * 24 * 60 * 60 * 1000);
                localTimers.put("notification", timer1);
            }
            if (schedule.getEndsOn() != null) {
                Timer timer2 = new Timer();
                TimerTask timerTask2 = new TimerTask() {
                    @Override
                    public void run() {
                        //stop other timers
                        timersState.get(schedule.getId()).get("wo_creation").cancel();
                        timersState.get(schedule.getId()).get("wo_creation").purge();
                        if (timersState.get(schedule.getId()).containsKey("notification")) {
                            timersState.get(schedule.getId()).get("notification").cancel();
                            timersState.get(schedule.getId()).get("notification").purge();
                        }
                    }
                };
                timer2.schedule(timerTask2, schedule.getEndsOn());
                localTimers.put("stop", timer2); //third schedule stopping
            }
            timersState.put(schedule.getId(), localTimers);
        }
    }

    public void reScheduleWorkOrder(Long id, Schedule schedule) {
        stopScheduleTimers(id);
        scheduleWorkOrder(schedule);
    }

    public void stopScheduleTimers(Long id) {
        if (!timersState.containsKey(id)) {
            return;
        }
        timersState.get(id).forEach((key, timer) -> {
            timer.cancel();
            timer.purge();
        });
    }

    public Schedule save(Schedule schedule) {
        return scheduleRepository.saveAndFlush(schedule);
    }
}
