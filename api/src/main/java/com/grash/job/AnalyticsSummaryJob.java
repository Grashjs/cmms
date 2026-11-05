package com.grash.job;

import com.grash.service.AnalyticsSummaryEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Runs every hour (see QuartzConfig) and emails the weekly and monthly maintenance summaries that are due in the time
 * zone of each company.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@DisallowConcurrentExecution
public class AnalyticsSummaryJob implements Job {

    private final AnalyticsSummaryEmailService analyticsSummaryEmailService;

    @Override
    public void execute(JobExecutionContext context) {
        // the scheduled time rather than now, so that a run that starts a little late still counts for its hour
        Instant fireTime = context.getScheduledFireTime() == null ? Instant.now() :
                context.getScheduledFireTime().toInstant();
        int sent = analyticsSummaryEmailService.sendDue(fireTime);
        if (sent > 0) log.info("Sent {} maintenance summary emails", sent);
    }
}
