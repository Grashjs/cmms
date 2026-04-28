package com.grash.job;

import com.grash.service.LdapService;
import com.grash.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class LdapSyncJob implements Job {

    private final LdapService ldapService;

    @Override
    @Transactional
    public void execute(JobExecutionContext context) {
        log.info("Starting LDAP user sync job");
        try {
            ldapService.syncLdapUsers();
            log.info("LDAP user sync completed successfully");
        } catch (Exception e) {
            log.error("LDAP user sync failed", e);
        }
    }
}
