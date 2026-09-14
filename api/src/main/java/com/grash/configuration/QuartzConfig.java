package com.grash.configuration;

import com.grash.job.DeleteDemoCompaniesJob;
import io.sentry.Sentry;
import org.quartz.*;
import org.quartz.listeners.JobListenerSupport;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail deleteDemoCompaniesJobDetail() {
        return JobBuilder.newJob(DeleteDemoCompaniesJob.class)
                .withIdentity("deleteDemoCompaniesJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger deleteDemoCompaniesTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(deleteDemoCompaniesJobDetail())
                .withIdentity("deleteDemoCompaniesTrigger")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInHours(1)
                        .repeatForever())
                .build();
    }

    @Bean
    public SchedulerFactoryBeanCustomizer sentryQuartzSchedulerCustomizer() {
        return schedulerFactoryBean -> schedulerFactoryBean.setGlobalJobListeners(new SentryJobListener());
    }

    static class SentryJobListener extends JobListenerSupport {

        @Override
        public String getName() {
            return "SentryJobListener";
        }

        @Override
        public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
            if (jobException != null) {
                Sentry.captureException(jobException);
            }
        }
    }
}
