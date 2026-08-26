package com.grash.configuration;

import org.junit.jupiter.api.Test;
import org.quartz.JobDetail;
import org.quartz.Trigger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuartzConfigTest {

    private final QuartzConfig quartzConfig = new QuartzConfig();

    @Test
    void deleteDemoCompaniesJobDetail_hasExpectedIdentityAndIsDurable() {
        JobDetail jobDetail = quartzConfig.deleteDemoCompaniesJobDetail();

        assertEquals("deleteDemoCompaniesJob", jobDetail.getKey().getName());
        assertTrue(jobDetail.isDurable());
    }

    @Test
    void deleteDemoCompaniesTrigger_hasExpectedIdentity() {
        Trigger trigger = quartzConfig.deleteDemoCompaniesTrigger();

        assertEquals("deleteDemoCompaniesTrigger", trigger.getKey().getName());
    }
}
