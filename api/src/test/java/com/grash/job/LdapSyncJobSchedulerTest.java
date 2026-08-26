package com.grash.job;

import com.grash.service.LdapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.springframework.boot.ApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LdapSyncJobSchedulerTest {

    @Mock
    private Scheduler scheduler;
    @Mock
    private LdapService ldapService;

    @InjectMocks
    private LdapSyncJobScheduler ldapSyncJobScheduler;

    private ApplicationArguments args;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ldapSyncJobScheduler, "ldapSyncEnabled", true);
        ReflectionTestUtils.setField(ldapSyncJobScheduler, "ldapEnabled", true);
        ReflectionTestUtils.setField(ldapSyncJobScheduler, "ldapSyncCron", "0 0 1 * * ?");
        args = mock(ApplicationArguments.class);
    }

    @Test
    void run_whenLdapSyncDisabled_doesNotSchedule() throws Exception {
        ReflectionTestUtils.setField(ldapSyncJobScheduler, "ldapSyncEnabled", false);

        ldapSyncJobScheduler.run(args);

        verify(scheduler, never()).scheduleJob(any(JobDetail.class), any(Trigger.class));
        verify(scheduler, never()).checkExists(any(JobKey.class));
        verify(ldapService, never()).syncLdapUsers();
    }

    @Test
    void run_whenLdapDisabled_doesNotSchedule() throws Exception {
        ReflectionTestUtils.setField(ldapSyncJobScheduler, "ldapEnabled", false);

        ldapSyncJobScheduler.run(args);

        verify(scheduler, never()).scheduleJob(any(JobDetail.class), any(Trigger.class));
        verify(scheduler, never()).checkExists(any(JobKey.class));
        verify(ldapService, never()).syncLdapUsers();
    }

    @Test
    void run_whenEnabled_schedulesJobAndRunsInitialSync() throws Exception {
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);

        ldapSyncJobScheduler.run(args);

        ArgumentCaptor<JobDetail> jobCaptor = ArgumentCaptor.forClass(JobDetail.class);
        ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
        verify(scheduler).scheduleJob(jobCaptor.capture(), triggerCaptor.capture());
        assertEquals(new JobKey("ldap-sync-job", "ldap-sync-group"), jobCaptor.getValue().getKey());
        assertEquals(new TriggerKey("ldap-sync-trigger", "ldap-sync-group"), triggerCaptor.getValue().getKey());
        verify(scheduler, never()).deleteJob(any(JobKey.class));
        verify(ldapService).syncLdapUsers();
    }

    @Test
    void run_whenJobAlreadyExists_deletesBeforeRescheduling() throws Exception {
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);

        ldapSyncJobScheduler.run(args);

        verify(scheduler).deleteJob(new JobKey("ldap-sync-job", "ldap-sync-group"));
        verify(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
        verify(ldapService).syncLdapUsers();
    }

    @Test
    void run_whenCheckExistsThrows_swallowsException() throws Exception {
        when(scheduler.checkExists(any(JobKey.class))).thenThrow(new SchedulerException("boom"));

        assertDoesNotThrow(() -> ldapSyncJobScheduler.run(args));

        verify(scheduler, never()).scheduleJob(any(JobDetail.class), any(Trigger.class));
        verify(ldapService, never()).syncLdapUsers();
    }

    @Test
    void run_whenScheduleJobThrows_swallowsException() throws Exception {
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);
        when(scheduler.scheduleJob(any(JobDetail.class), any(Trigger.class)))
                .thenThrow(new SchedulerException("boom"));

        assertDoesNotThrow(() -> ldapSyncJobScheduler.run(args));

        verify(ldapService, never()).syncLdapUsers();
    }
}
