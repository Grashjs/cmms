package com.grash.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import java.util.Properties;

@Configuration
public class TestQuartzConfig {

    @Bean
    @Primary
    public SchedulerFactoryBean schedulerFactoryBean() {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        Properties props = new Properties();
        props.setProperty("org.quartz.scheduler.instanceName", "TestScheduler");
        props.setProperty("org.quartz.threadPool.threadCount", "1");
        props.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
        factory.setQuartzProperties(props);
        return factory;
    }
}
