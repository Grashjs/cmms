package com.grash.automation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * The rule engine gets its own executor, and this is not a preference.
 *
 * <p>{@code AsyncConfig} defines the application-wide default: three threads, a queue of eleven,
 * shared by CSV exports, imports, mail, comment notifications, demo data and request triage. An
 * export runs for minutes. Rule evaluation fires on <em>every</em> entity change, so putting it
 * on that pool means queueing behind an export and, once the queue is full, being rejected by
 * the default {@code AbortPolicy} — a {@code TaskRejectedException} the caller never sees,
 * because the publisher has long since returned. Runs would vanish under load, which is the one
 * failure mode an automation engine must not have.
 *
 * <p>{@code CallerRunsPolicy} is the other half: when the queue is full the publishing thread
 * executes the run itself. That makes load push back on whoever is causing it instead of
 * silently dropping work. It costs latency in the request that triggered the rule, which is the
 * right thing to pay when the alternative is losing the run.
 */
@Configuration
public class AutomationAsyncConfig {

    public static final String EXECUTOR = "automationExecutor";

    @Bean(EXECUTOR)
    public Executor automationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("automation-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        return executor;
    }
}
