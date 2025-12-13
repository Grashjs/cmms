package com.grash.job;

import com.grash.model.OwnUser;
import com.grash.model.PreventiveMaintenance;
import com.grash.model.Schedule;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RecurrenceType;
import com.grash.repository.ScheduleRepository;
import com.grash.service.EmailService2;
import com.grash.service.ScheduleService;
import com.grash.service.UserService;
import com.grash.utils.Helper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class PreventiveMaintenanceNotificationJob extends QuartzJobBean {

    private final ScheduleRepository scheduleRepository;
    private final UserService userService;
    private final EmailService2 emailService2;
    private final MessageSource messageSource;
    private final ScheduleService scheduleService;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    @Transactional
    public void executeInternal(JobExecutionContext context) throws JobExecutionException {
        Long scheduleId = context.getMergedJobDataMap().getLong("scheduleId");

        Schedule schedule = scheduleRepository.findById(scheduleId).orElse(null);
        if (schedule == null || schedule.isDisabled()) {
            return;
        }
        scheduleService.checkIfWeeklyShouldRun(schedule);

        PreventiveMaintenance preventiveMaintenance = schedule.getPreventiveMaintenance();
        Locale locale = Helper.getLocale(preventiveMaintenance.getCompany());
        String title = messageSource.getMessage("coming_wo", null, locale);

        // Logic copied from original TimerTask
        Collection<OwnUser> admins = userService.findWorkersByCompany(preventiveMaintenance.getCompany().getId())
                .stream()
                .filter(ownUser -> ownUser.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS))
                .collect(Collectors.toList());

        List<OwnUser> usersToMail = new ArrayList<>(Stream.concat(
                        preventiveMaintenance.getUsers().stream(),
                        admins.stream())
                .filter(user -> user.isEnabled() && user.getUserSettings().shouldEmailUpdatesForWorkOrders())
                .collect(Collectors.toMap(
                        OwnUser::getId,
                        Function.identity(),
                        (existing, replacement) -> existing))
                .values());

        Map<String, Object> mailVariables = new HashMap<String, Object>() {{
            put("pmLink", frontendUrl + "/app/preventive-maintenances/" + preventiveMaintenance.getId());
            put("featuresLink", frontendUrl + "/#key-features");
            put("pmTitle", preventiveMaintenance.getTitle());
        }};

        emailService2.sendMessageUsingThymeleafTemplate(
                usersToMail.stream().map(OwnUser::getEmail).toArray(String[]::new),
                title,
                mailVariables,
                "coming-work-order.html",
                locale
        );
    }
}