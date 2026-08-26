package com.grash.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class AsyncConfigTest {

    private final AsyncConfig asyncConfig = new AsyncConfig();

    @Test
    void getAsyncExecutor_returnsConfiguredThreadPool() {
        Executor executor = asyncConfig.getAsyncExecutor();

        ThreadPoolTaskExecutor taskExecutor = assertInstanceOf(ThreadPoolTaskExecutor.class, executor);
        assertEquals(3, taskExecutor.getCorePoolSize());
        assertEquals(3, taskExecutor.getMaxPoolSize());
        assertEquals(11, taskExecutor.getQueueCapacity());
        assertEquals("MyExecutor-", taskExecutor.getThreadNamePrefix());
    }
}
