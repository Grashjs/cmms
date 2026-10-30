package com.grash.job;

import com.grash.service.AnalyticsSummaryEmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;

import java.time.Instant;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsSummaryJobTest {

    @Mock
    private AnalyticsSummaryEmailService analyticsSummaryEmailService;

    @InjectMocks
    private AnalyticsSummaryJob analyticsSummaryJob;

    @Test
    void execute_sendsTheSummariesDueAtTheScheduledTime() {
        Instant scheduled = Instant.parse("2026-10-26T07:00:00Z");
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getScheduledFireTime()).thenReturn(Date.from(scheduled));

        analyticsSummaryJob.execute(context);

        verify(analyticsSummaryEmailService).sendDue(scheduled);
    }

    @Test
    void execute_withoutScheduledTime_usesNow() {
        analyticsSummaryJob.execute(mock(JobExecutionContext.class));

        verify(analyticsSummaryEmailService).sendDue(any(Instant.class));
    }
}
