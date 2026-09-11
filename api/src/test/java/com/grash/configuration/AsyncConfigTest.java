package com.grash.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;

class AsyncConfigTest {

    private final AsyncConfig asyncConfig = new AsyncConfig();

    @Test
    void getAsyncExecutor_wrapsConfiguredThreadPool() {
        Executor executor = asyncConfig.getAsyncExecutor();

        DelegatingSecurityContextAsyncTaskExecutor delegating =
                assertInstanceOf(DelegatingSecurityContextAsyncTaskExecutor.class, executor);
        ThreadPoolTaskExecutor taskExecutor =
                (ThreadPoolTaskExecutor) ReflectionTestUtils.getField(delegating, "delegate");

        assertNotNull(taskExecutor);
        assertEquals(3, taskExecutor.getCorePoolSize());
        assertEquals(3, taskExecutor.getMaxPoolSize());
        assertEquals(11, taskExecutor.getQueueCapacity());
        assertEquals("MyExecutor-", taskExecutor.getThreadNamePrefix());
    }
}