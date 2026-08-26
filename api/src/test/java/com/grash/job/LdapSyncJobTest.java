package com.grash.job;

import com.grash.service.LdapService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LdapSyncJobTest {

    @Mock
    private LdapService ldapService;

    @InjectMocks
    private LdapSyncJob ldapSyncJob;

    @Test
    void execute_syncsLdapUsers() {
        JobExecutionContext context = mock(JobExecutionContext.class);

        assertDoesNotThrow(() -> ldapSyncJob.execute(context));

        verify(ldapService).syncLdapUsers();
    }

    @Test
    void execute_whenSyncFails_swallowsException() {
        doThrow(new RuntimeException("LDAP down")).when(ldapService).syncLdapUsers();
        JobExecutionContext context = mock(JobExecutionContext.class);

        assertDoesNotThrow(() -> ldapSyncJob.execute(context));

        verify(ldapService).syncLdapUsers();
    }
}
