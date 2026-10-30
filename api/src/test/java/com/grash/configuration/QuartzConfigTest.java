package com.grash.configuration;

import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Trigger;

import java.util.TimeZone;

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

    @Test
    void analyticsSummaryJobDetail_hasExpectedIdentityAndIsDurable() {
        JobDetail jobDetail = quartzConfig.analyticsSummaryJobDetail();

        assertEquals("analyticsSummaryJob", jobDetail.getKey().getName());
        assertTrue(jobDetail.isDurable());
    }

    @Test
    void analyticsSummaryTrigger_firesEveryHourAndSkipsMisfires() {
        CronTrigger trigger = (CronTrigger) quartzConfig.analyticsSummaryTrigger();

        assertEquals("analyticsSummaryTrigger", trigger.getKey().getName());
        assertEquals("analyticsSummaryJob", trigger.getJobKey().getName());
        assertEquals("0 0 * * * ?", trigger.getCronExpression());
        assertEquals(TimeZone.getTimeZone("UTC"), trigger.getTimeZone());
        assertEquals(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING, trigger.getMisfireInstruction());
    }
}
