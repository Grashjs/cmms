package com.grash.job;

import com.grash.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.TimeZone;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class LdapSyncJobScheduler implements ApplicationRunner {

    private final Scheduler scheduler;
    private final UserService userService;

    @Value("${ldap.sync.enabled}")
    private boolean ldapSyncEnabled;
    @Value("${ldap.enabled}")
    private boolean ldapEnabled;

    @Value("${ldap.sync.cron}")
    private String ldapSyncCron;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!ldapSyncEnabled || !ldapEnabled) {
            return;
        }

        log.info("Scheduling LDAP sync job with cron: {}", ldapSyncCron);

        JobDetail jobDetail = JobBuilder.newJob(LdapSyncJob.class)
                .withIdentity("ldap-sync-job", "ldap-sync-group")
                .build();

        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("ldap-sync-trigger", "ldap-sync-group")
                .withSchedule(CronScheduleBuilder.cronSchedule(ldapSyncCron)
                        .inTimeZone(TimeZone.getDefault()))
                .build();

        try {
            if (scheduler.checkExists(jobDetail.getKey())) {
                scheduler.deleteJob(jobDetail.getKey());
            }
            scheduler.scheduleJob(jobDetail, trigger);

            log.info("LDAP sync job scheduled successfully");

            log.info("Running initial LDAP sync on startup");
            userService.syncLdapUsers();
            log.info("Initial LDAP sync completed successfully");

        } catch (SchedulerException e) {
            log.error("Error scheduling LDAP sync job", e);
        }
    }
}
