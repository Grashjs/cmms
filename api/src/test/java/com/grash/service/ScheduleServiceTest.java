package com.grash.service;

import com.grash.dto.SchedulePatchDTO;
import com.grash.exception.CustomException;
import com.grash.job.PreventiveMaintenanceNotificationJob;
import com.grash.job.WorkOrderCreationJob;
import com.grash.mapper.ScheduleMapper;
import com.grash.model.Company;
import com.grash.model.CompanySettings;
import com.grash.model.GeneralPreferences;
import com.grash.model.PreventiveMaintenance;
import com.grash.model.Schedule;
import com.grash.model.Subscription;
import com.grash.model.SubscriptionPlan;
import com.grash.model.WorkOrder;
import com.grash.model.enums.PlanFeatures;
import com.grash.model.enums.RecurrenceBasedOn;
import com.grash.model.enums.RecurrenceType;
import com.grash.model.enums.Status;
import com.grash.repository.ScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.CalendarIntervalTrigger;
import org.quartz.CronTrigger;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    private static final String TIME_ZONE = "UTC";
    private static final long DAY_MS = 86_400_000L;
    private static final long PM_ID = 10L;
    private static final long SCHEDULE_ID = 1L;
    private static final int LAST_PM_LIMIT = 5;
    // 2026-03-02 is a Monday, 2026-02-28 a Saturday and 2026-02-27 a Friday; all at 09:15 UTC.
    private static final Instant MONDAY_0915 = Instant.parse("2026-03-02T09:15:00Z");
    private static final Instant SATURDAY_0915 = Instant.parse("2026-02-28T09:15:00Z");
    private static final Instant FRIDAY_0915 = Instant.parse("2026-02-27T09:15:00Z");

    @InjectMocks
    private ScheduleService scheduleService;

    @Mock
    private ScheduleRepository scheduleRepository;
    @Mock
    private ScheduleMapper scheduleMapper;
    @Mock
    private WorkOrderService workOrderService;
    @Mock
    private Scheduler scheduler;

    private Company company;
    private PreventiveMaintenance preventiveMaintenance;

    @BeforeEach
    void setUp() {
        CompanySettings companySettings = new CompanySettings();
        companySettings.setId(1L);
        GeneralPreferences generalPreferences = new GeneralPreferences(companySettings);
        generalPreferences.setTimeZone(TIME_ZONE);
        // Default to "no notification job" so scheduling assertions stay focused.
        generalPreferences.setDaysBeforePrevMaintNotification(0);
        companySettings.setGeneralPreferences(generalPreferences);

        SubscriptionPlan plan = SubscriptionPlan.builder()
                .id(1L)
                .name("Pro")
                .features(new HashSet<>(Collections.singletonList(PlanFeatures.PREVENTIVE_MAINTENANCE)))
                .build();
        Subscription subscription = Subscription.builder().id(1L).subscriptionPlan(plan).build();
        company = new Company("TestCo", 10, subscription);
        company.setId(1L);
        company.setCompanySettings(companySettings);

        preventiveMaintenance = new PreventiveMaintenance();
        preventiveMaintenance.setId(PM_ID);
        preventiveMaintenance.setName("PM");
        preventiveMaintenance.setCompany(company);

        lenient().when(workOrderService.findLastByPM(anyLong(), anyInt()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
    }

    // ─── Helpers ───────────────────────────────────────────────────────

    private static final class ScheduledJob {
        private final JobDetail job;
        private final Trigger trigger;

        private ScheduledJob(JobDetail job, Trigger trigger) {
            this.job = job;
            this.trigger = trigger;
        }
    }

    private Schedule buildSchedule() {
        Schedule schedule = new Schedule(preventiveMaintenance);
        schedule.setId(SCHEDULE_ID);
        schedule.setStartsOn(new Date());
        schedule.setRecurrenceType(RecurrenceType.DAILY);
        schedule.setRecurrenceBasedOn(RecurrenceBasedOn.SCHEDULED_DATE);
        schedule.setFrequency(1);
        schedule.setDaysOfWeek(new ArrayList<>());
        schedule.setDisabled(false);
        return schedule;
    }

    private WorkOrder buildWorkOrder(Long id, Status status, Date completedOn, Date firstTimeToReact) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(id);
        workOrder.setStatus(status);
        workOrder.setCompletedOn(completedOn);
        workOrder.setFirstTimeToReact(firstTimeToReact);
        return workOrder;
    }

    private WorkOrder completedWorkOrder(Long id, Date completedOn) {
        return buildWorkOrder(id, Status.COMPLETE, completedOn, new Date());
    }

    private Page<WorkOrder> pageOf(WorkOrder... workOrders) {
        return new PageImpl<>(Arrays.asList(workOrders));
    }

    private SchedulePatchDTO patchDto() {
        SchedulePatchDTO dto = new SchedulePatchDTO();
        dto.setRecurrenceType(RecurrenceType.DAILY);
        dto.setRecurrenceBasedOn(RecurrenceBasedOn.SCHEDULED_DATE);
        return dto;
    }

    private static Date daysFromNow(int days) {
        return new Date(System.currentTimeMillis() + days * DAY_MS);
    }

    private void setDaysBeforeNotification(int days) {
        company.getCompanySettings().getGeneralPreferences().setDaysBeforePrevMaintNotification(days);
    }

    private List<ScheduledJob> scheduledJobs() throws SchedulerException {
        ArgumentCaptor<JobDetail> jobs = ArgumentCaptor.forClass(JobDetail.class);
        ArgumentCaptor<Trigger> triggers = ArgumentCaptor.forClass(Trigger.class);
        verify(scheduler, atLeastOnce()).scheduleJob(jobs.capture(), triggers.capture());
        List<ScheduledJob> result = new ArrayList<>();
        List<JobDetail> allJobs = jobs.getAllValues();
        List<Trigger> allTriggers = triggers.getAllValues();
        for (int i = 0; i < allJobs.size(); i++) {
            result.add(new ScheduledJob(allJobs.get(i), allTriggers.get(i)));
        }
        return result;
    }

    private int scheduledCount() {
        return (int) mockingDetails(scheduler).getInvocations().stream()
                .filter(invocation -> "scheduleJob".equals(invocation.getMethod().getName()))
                .count();
    }

    private ScheduledJob jobNamed(String name) throws SchedulerException {
        return scheduledJobs().stream()
                .filter(scheduled -> scheduled.job.getKey().getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No job scheduled with name " + name));
    }

    private void verifyNoJobScheduled() throws SchedulerException {
        assertEquals(0, scheduledCount());
        verify(scheduler, never()).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }

    // ─── Simple repository pass-throughs ────────────────────────────────

    @Nested
    class Create {

        @Test
        void savesSchedule() throws SchedulerException {
            Schedule schedule = buildSchedule();
            when(scheduleRepository.save(schedule)).thenReturn(schedule);

            assertSame(schedule, scheduleService.create(schedule));
        }
    }

    @Nested
    class Update {

        @Test
        void existingSchedule_isMappedAndSaved() throws SchedulerException {
            Schedule stored = buildSchedule();
            Schedule mapped = buildSchedule();
            SchedulePatchDTO dto = patchDto();
            when(scheduleRepository.existsById(SCHEDULE_ID)).thenReturn(true);
            when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(stored));
            when(scheduleMapper.updateSchedule(stored, dto)).thenReturn(mapped);
            when(scheduleRepository.save(mapped)).thenReturn(mapped);

            assertSame(mapped, scheduleService.update(SCHEDULE_ID, dto));
        }

        @Test
        void missingSchedule_throwsNotFound() throws SchedulerException {
            when(scheduleRepository.existsById(SCHEDULE_ID)).thenReturn(false);

            CustomException ex = assertThrows(CustomException.class,
                    () -> scheduleService.update(SCHEDULE_ID, patchDto()));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
            verify(scheduleRepository, never()).findById(anyLong());
            verify(scheduleRepository, never()).save(any(Schedule.class));
        }
    }

    @Nested
    class Queries {

        @Test
        void getAll_delegatesToRepository() throws SchedulerException {
            List<Schedule> all = Collections.singletonList(buildSchedule());
            when(scheduleRepository.findAll()).thenReturn(all);

            assertSame(all, scheduleService.getAll());
        }

        @Test
        void findById_present() throws SchedulerException {
            Schedule schedule = buildSchedule();
            when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(schedule));

            assertEquals(Optional.of(schedule), scheduleService.findById(SCHEDULE_ID));
        }

        @Test
        void findById_absent() throws SchedulerException {
            when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.empty());

            assertTrue(scheduleService.findById(SCHEDULE_ID).isEmpty());
        }

        @Test
        void findByCompany_delegatesToRepository() throws SchedulerException {
            Collection<Schedule> schedules = Collections.singletonList(buildSchedule());
            when(scheduleRepository.findByCompany_Id(company.getId())).thenReturn(schedules);

            assertSame(schedules, scheduleService.findByCompany(company.getId()));
        }

        @Test
        void findActive_delegatesToRepository() throws SchedulerException {
            Collection<Schedule> schedules = Collections.singletonList(buildSchedule());
            when(scheduleRepository.findByActive()).thenReturn(schedules);

            assertSame(schedules, scheduleService.findActive());
        }

        @Test
        void save_usesSaveAndFlush() throws SchedulerException {
            Schedule schedule = buildSchedule();
            when(scheduleRepository.saveAndFlush(schedule)).thenReturn(schedule);

            assertSame(schedule, scheduleService.save(schedule));
        }

        @Test
        void deleteByCompanyIdAndIsDemoTrue_delegatesToRepository() throws SchedulerException {
            scheduleService.deleteByCompanyIdAndIsDemoTrue(company.getId());

            verify(scheduleRepository).deleteByPreventiveMaintenanceCompany_IdAndIsDemoTrue(company.getId());
        }

        @Test
        void disableByCompany_delegatesToRepository() throws SchedulerException {
            scheduleService.disableByCompany(company.getId());

            verify(scheduleRepository).updateDisabledTrueByCompanyId(company.getId());
        }
    }

    @Nested
    class Delete {

        @Test
        void stopsJobsBeforeDeletingRow() throws SchedulerException {
            scheduleService.delete(SCHEDULE_ID);

            verify(scheduler).deleteJob(new JobKey("wo-job-" + SCHEDULE_ID, "wo-group"));
            verify(scheduler).deleteJob(new JobKey("notif-job-" + SCHEDULE_ID, "notif-group"));
            verify(scheduleRepository).deleteById(SCHEDULE_ID);
        }
    }

    @Nested
    class StopScheduleJobs {

        @Test
        void deletesWorkOrderAndNotificationJobs() throws SchedulerException {
            scheduleService.stopScheduleJobs(SCHEDULE_ID);

            verify(scheduler).deleteJob(eq(new JobKey("wo-job-" + SCHEDULE_ID, "wo-group")));
            verify(scheduler).deleteJob(eq(new JobKey("notif-job-" + SCHEDULE_ID, "notif-group")));
        }

        @Test
        void schedulerException_isSwallowedAndStopsProcessing() throws SchedulerException {
            when(scheduler.deleteJob(new JobKey("wo-job-" + SCHEDULE_ID, "wo-group")))
                    .thenThrow(new SchedulerException("quartz down"));

            assertDoesNotThrow(() -> scheduleService.stopScheduleJobs(SCHEDULE_ID));

            verify(scheduler, never()).deleteJob(new JobKey("notif-job-" + SCHEDULE_ID, "notif-group"));
        }
    }

    @Nested
    class CheckIfWeeklyShouldRun {

        @Test
        void nonWeeklyRecurrence_alwaysRuns() throws SchedulerException {
            Schedule schedule = buildSchedule();
            schedule.setRecurrenceType(RecurrenceType.MONTHLY);
            schedule.setFrequency(3);

            assertTrue(scheduleService.checkIfWeeklyShouldRun(schedule));
        }

        @Test
        void weeklyWithFrequencyOne_alwaysRuns() throws SchedulerException {
            Schedule schedule = buildSchedule();
            schedule.setRecurrenceType(RecurrenceType.WEEKLY);
            schedule.setFrequency(1);

            assertTrue(scheduleService.checkIfWeeklyShouldRun(schedule));
        }

        @Test
        void weeklyOnMatchingWeekInterval_runs() throws SchedulerException {
            Schedule schedule = buildSchedule();
            schedule.setRecurrenceType(RecurrenceType.WEEKLY);
            schedule.setFrequency(2);
            // 15 days -> 2 whole weeks -> 2 % 2 == 0
            schedule.setStartsOn(daysFromNow(-15));

            assertTrue(scheduleService.checkIfWeeklyShouldRun(schedule));
        }

        @Test
        void weeklyOffInterval_isSkipped() throws SchedulerException {
            Schedule schedule = buildSchedule();
            schedule.setRecurrenceType(RecurrenceType.WEEKLY);
            schedule.setFrequency(2);
            // 8 days -> 1 whole week -> 1 % 2 != 0
            schedule.setStartsOn(daysFromNow(-8));

            assertFalse(scheduleService.checkIfWeeklyShouldRun(schedule));
        }
    }

    @Nested
    class ScheduleWorkOrderGuards {

        @Test
        void disabledSchedule_isNotScheduled() throws SchedulerException {
            Schedule schedule = buildSchedule();
            schedule.setDisabled(true);

            scheduleService.scheduleWorkOrder(schedule);

            verifyNoJobScheduled();
        }

        @Test
        void scheduleEndedInThePast_isNotScheduled() throws SchedulerException {
            Schedule schedule = buildSchedule();
            schedule.setEndsOn(daysFromNow(-1));

            scheduleService.scheduleWorkOrder(schedule);

            verifyNoJobScheduled();
        }

        @Test
        void staleSchedule_isDisabledAndNotScheduled() throws SchedulerException {
            Schedule schedule = buildSchedule();
            WorkOrder[] unreacted = new WorkOrder[LAST_PM_LIMIT];
            for (int i = 0; i < LAST_PM_LIMIT; i++) {
                unreacted[i] = buildWorkOrder((long) i, Status.OPEN, null, null);
            }
            when(workOrderService.findLastByPM(PM_ID, LAST_PM_LIMIT)).thenReturn(pageOf(unreacted));
            when(scheduleRepository.save(schedule)).thenReturn(schedule);

            scheduleService.scheduleWorkOrder(schedule);

            assertTrue(schedule.isDisabled());
            verify(scheduleRepository).save(schedule);
            verifyNoJobScheduled();
        }

        @Test
        void notStale_whenAnyWorkOrderWasReactedTo_isScheduled() throws SchedulerException {
            Schedule schedule = buildSchedule();
            WorkOrder[] workOrders = new WorkOrder[LAST_PM_LIMIT];
            for (int i = 0; i < LAST_PM_LIMIT; i++) {
                workOrders[i] = buildWorkOrder((long) i, Status.COMPLETE, new Date(), null);
            }
            workOrders[0] = buildWorkOrder(0L, Status.COMPLETE, new Date(), new Date());
            when(workOrderService.findLastByPM(PM_ID, LAST_PM_LIMIT)).thenReturn(pageOf(workOrders));

            scheduleService.scheduleWorkOrder(schedule);

            assertFalse(schedule.isDisabled());
            verify(scheduleRepository, never()).save(any(Schedule.class));
            assertEquals(1, scheduledCount());
        }

        @Test
        void endsOnNotAfterStartsOn_isSkipped() throws SchedulerException {
            Schedule schedule = buildSchedule();
            schedule.setEndsOn(daysFromNow(1));
            schedule.setStartsOn(daysFromNow(10));

            scheduleService.scheduleWorkOrder(schedule);

            verifyNoJobScheduled();
        }

        @Test
        void weeklyWithoutDaysOfWeek_throwsBadRequest() throws SchedulerException {
            Schedule schedule = buildSchedule();
            schedule.setRecurrenceType(RecurrenceType.WEEKLY);
            schedule.setDaysOfWeek(new ArrayList<>());

            CustomException ex = assertThrows(CustomException.class,
                    () -> scheduleService.scheduleWorkOrder(schedule));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
            verifyNoJobScheduled();
        }

        @Test
        void weeklyWithNullDaysOfWeek_throwsBadRequest() throws SchedulerException {
            Schedule schedule = buildSchedule();
            schedule.setRecurrenceType(RecurrenceType.WEEKLY);
            schedule.setDaysOfWeek(null);

            CustomException ex = assertThrows(CustomException.class,
                    () -> scheduleService.scheduleWorkOrder(schedule));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
            verifyNoJobScheduled();
        }

        @Test
        void schedulerException_isNotSwallowed() throws SchedulerException {
            Schedule schedule = buildSchedule();
            when(scheduler.scheduleJob(any(JobDetail.class), any(Trigger.class)))
                    .thenThrow(new SchedulerException("quartz down"));

            assertThrows(CustomException.class, () -> scheduleService.scheduleWorkOrder(schedule));

            verify(scheduler, times(1)).scheduleJob(any(JobDetail.class), any(Trigger.class));
        }
    }

    @Nested
    class ScheduleWorkOrderByRecurrenceType {

        @Test
        void daily_buildsRepeatForeverHourlyTrigger() throws SchedulerException {
            Schedule schedule = buildSchedule();
            schedule.setRecurrenceType(RecurrenceType.DAILY);
            schedule.setFrequency(3);

            scheduleService.scheduleWorkOrder(schedule);

            ScheduledJob scheduled = jobNamed("wo-job-" + SCHEDULE_ID);
            assertEquals(WorkOrderCreationJob.class, scheduled.job.getJobClass());
            assertEquals("wo-group", scheduled.job.getKey().getGroup());
            assertTrue(scheduled.job.isDurable());
            assertEquals(SCHEDULE_ID, scheduled.job.getJobDataMap().getLong("scheduleId"));
            assertEquals("wo-trigger-" + SCHEDULE_ID, scheduled.trigger.getKey().getName());
            assertEquals(schedule.getStartsOn(), scheduled.trigger.getStartTime());
            assertNull(scheduled.trigger.getEndTime());

            SimpleTrigger trigger = (SimpleTrigger) scheduled.trigger;
            assertEquals(TimeUnit.HOURS.toMillis(24L * 3), trigger.getRepeatInterval());
            assertEquals(-1, trigger.getRepeatCount());
            assertEquals(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW, trigger.getMisfireInstruction());
        }

        @Test
        void monthly_buildsCalendarIntervalTriggerInMonths() throws SchedulerException {
            Schedule schedule = buildSchedule();
            schedule.setRecurrenceType(RecurrenceType.MONTHLY);
            schedule.setFrequency(6);

            scheduleService.scheduleWorkOrder(schedule);

            Trigger trigger = jobNamed("wo-job-" + SCHEDULE_ID).trigger;
            assertInstanceOf(CalendarIntervalTrigger.class, trigger);
            assertEquals(6, ((CalendarIntervalTrigger) trigger).getRepeatInterval());
            assertEquals(IntervalUnit.MONTH, ((CalendarIntervalTrigger) trigger).getRepeatIntervalUnit());
        }

        @Test
        void yearly_buildsCalendarIntervalTriggerInYears() throws SchedulerException {
            Schedule schedule = buildSchedule();
            schedule.setRecurrenceType(RecurrenceType.YEARLY);
            schedule.setFrequency(2);

            scheduleService.scheduleWorkOrder(schedule);

            Trigger trigger = jobNamed("wo-job-" + SCHEDULE_ID).trigger;
            assertInstanceOf(CalendarIntervalTrigger.class, trigger);
            assertEquals(2, ((CalendarIntervalTrigger) trigger).getRepeatInterval());
            assertEquals(IntervalUnit.YEAR, ((CalendarIntervalTrigger) trigger).getRepeatIntervalUnit());
        }

        @Test
        void weekly_buildsCronTriggerWithConvertedDaysOfWeek() throws SchedulerException {
            Schedule schedule = buildSchedule();
            schedule.setRecurrenceType(RecurrenceType.WEEKLY);
            schedule.setFrequency(1);
            schedule.setStartsOn(Date.from(MONDAY_0915));
            schedule.setDaysOfWeek(Collections.singletonList(0)); // ISO Monday

            scheduleService.scheduleWorkOrder(schedule);

            ScheduledJob scheduled = jobNamed("wo-job-" + SCHEDULE_ID);
            CronTrigger trigger = (CronTrigger) scheduled.trigger;
            // Quartz numbering: Sun=1, Mon=2
            assertEquals("0 15 9 ? * 2", trigger.getCronExpression());
            assertEquals("UTC", trigger.getTimeZone().getID());
        }

        @Test
        void weekly_sundayMapsToQuartzDayOne() throws SchedulerException {
            Schedule schedule = buildSchedule();
            schedule.setRecurrenceType(RecurrenceType.WEEKLY);
            schedule.setStartsOn(Date.from(MONDAY_0915));
            schedule.setDaysOfWeek(Arrays.asList(0, 6)); // Monday + Sunday

            scheduleService.scheduleWorkOrder(schedule);

            CronTrigger trigger = (CronTrigger) jobNamed("wo-job-" + SCHEDULE_ID).trigger;
            // ISO Mon(0)->2, Sun(6)->1, order preserved from the configured list
            assertEquals("0 15 9 ? * 2,1", trigger.getCronExpression());
        }

        @Test
        void endsOnAfterStartsOn_setsTriggerEndTime() throws SchedulerException {
            Schedule schedule = buildSchedule();
            Date endsOn = daysFromNow(30);
            schedule.setEndsOn(endsOn);

            scheduleService.scheduleWorkOrder(schedule);

            assertEquals(endsOn, jobNamed("wo-job-" + SCHEDULE_ID).trigger.getEndTime());
        }
    }

    @Nested
    class ScheduleWorkOrderNotification {

        @Test
        void notificationsDisabled_schedulesOnlyWorkOrderJob() throws SchedulerException {
            Schedule schedule = buildSchedule();

            scheduleService.scheduleWorkOrder(schedule);

            assertEquals(1, scheduledCount());
        }

        @Test
        void notificationsEnabled_schedulesOffsetNotificationJob() throws SchedulerException {
            setDaysBeforeNotification(3);
            Schedule schedule = buildSchedule();
            schedule.setStartsOn(Date.from(MONDAY_0915));

            scheduleService.scheduleWorkOrder(schedule);

            assertEquals(2, scheduledCount());
            ScheduledJob notif = jobNamed("notif-job-" + SCHEDULE_ID);
            assertEquals(PreventiveMaintenanceNotificationJob.class, notif.job.getJobClass());
            assertEquals("notif-group", notif.job.getKey().getGroup());
            assertFalse(notif.job.isDurable());
            assertEquals(SCHEDULE_ID, notif.job.getJobDataMap().getLong("scheduleId"));
            assertEquals("notif-trigger-" + SCHEDULE_ID, notif.trigger.getKey().getName());
            // Monday 09:15 UTC minus 3 days -> Friday 09:15 UTC
            assertEquals(Date.from(FRIDAY_0915), notif.trigger.getStartTime());
        }

        @Test
        void estimatedStartDate_drivesNotificationOffset() throws SchedulerException {
            setDaysBeforeNotification(2);
            Schedule schedule = buildSchedule();
            schedule.setStartsOn(new Date());
            preventiveMaintenance.setEstimatedStartDate(Date.from(MONDAY_0915));

            scheduleService.scheduleWorkOrder(schedule);

            assertEquals(2, scheduledCount());
            ScheduledJob notif = jobNamed("notif-job-" + SCHEDULE_ID);
            assertEquals(Date.from(MONDAY_0915.minus(2, ChronoUnit.DAYS)), notif.trigger.getStartTime());
        }

        @Test
        void weeklyNotification_shiftsDaysWithWrapAround() throws SchedulerException {
            setDaysBeforeNotification(3);
            Schedule schedule = buildSchedule();
            schedule.setRecurrenceType(RecurrenceType.WEEKLY);
            schedule.setStartsOn(Date.from(MONDAY_0915));
            schedule.setDaysOfWeek(Collections.singletonList(0)); // ISO Monday

            scheduleService.scheduleWorkOrder(schedule);

            assertEquals(2, scheduledCount());
            CronTrigger notifTrigger = (CronTrigger) jobNamed("notif-job-" + SCHEDULE_ID).trigger;
            // Monday(0) - 3 days wraps to ISO Friday(4) -> Quartz day 6
            assertEquals("0 15 9 ? * 6", notifTrigger.getCronExpression());
            assertEquals(Date.from(FRIDAY_0915), notifTrigger.getStartTime());
        }

        @Test
        void weeklyNotification_shiftsDaysWithoutWrapAround() throws SchedulerException {
            setDaysBeforeNotification(2);
            Schedule schedule = buildSchedule();
            schedule.setRecurrenceType(RecurrenceType.WEEKLY);
            schedule.setStartsOn(Date.from(MONDAY_0915));
            schedule.setDaysOfWeek(Collections.singletonList(5)); // ISO Saturday

            scheduleService.scheduleWorkOrder(schedule);

            assertEquals(2, scheduledCount());
            // Saturday(5) - 2 days stays positive -> ISO Thursday(3) -> Quartz day 5
            assertEquals("0 15 9 ? * 7", ((CronTrigger) jobNamed("wo-job-" + SCHEDULE_ID).trigger)
                    .getCronExpression());
            CronTrigger notifTrigger = (CronTrigger) jobNamed("notif-job-" + SCHEDULE_ID).trigger;
            assertEquals("0 15 9 ? * 5", notifTrigger.getCronExpression());
            assertEquals(Date.from(SATURDAY_0915), notifTrigger.getStartTime());
        }

        @Test
        void monthlyReusesWorkOrderScheduleForNotification() throws SchedulerException {
            setDaysBeforeNotification(1);
            Schedule schedule = buildSchedule();
            schedule.setRecurrenceType(RecurrenceType.MONTHLY);
            schedule.setFrequency(3);
            schedule.setStartsOn(Date.from(MONDAY_0915));

            scheduleService.scheduleWorkOrder(schedule);

            assertEquals(2, scheduledCount());
            CalendarIntervalTrigger notifTrigger =
                    (CalendarIntervalTrigger) jobNamed("notif-job-" + SCHEDULE_ID).trigger;
            assertEquals(3, notifTrigger.getRepeatInterval());
            assertEquals(IntervalUnit.MONTH, notifTrigger.getRepeatIntervalUnit());
            assertEquals(Date.from(MONDAY_0915.minus(1, ChronoUnit.DAYS)), notifTrigger.getStartTime());
        }

        @Test
        void notificationStartAfterEndsOn_isSkipped() throws SchedulerException {
            setDaysBeforeNotification(3);
            Schedule schedule = buildSchedule();
            schedule.setStartsOn(new Date());
            schedule.setEndsOn(daysFromNow(1));
            // Estimated start is far enough ahead that notif start (est - 3d) lands after endsOn.
            preventiveMaintenance.setEstimatedStartDate(daysFromNow(10));

            scheduleService.scheduleWorkOrder(schedule);

            assertEquals(1, scheduledCount());
            assertNotNull(jobNamed("wo-job-" + SCHEDULE_ID));
        }
    }

    @Nested
    class ScheduleWorkOrderBasedOnCompletedDate {

        @Test
        void noWorkOrdersYet_schedulesOneShotJob() throws SchedulerException {
            Schedule schedule = buildSchedule();
            schedule.setRecurrenceBasedOn(RecurrenceBasedOn.COMPLETED_DATE);

            scheduleService.scheduleWorkOrder(schedule);

            assertEquals(1, scheduledCount());
            ScheduledJob scheduled = jobNamed("wo-job-" + SCHEDULE_ID);
            assertEquals(0, ((SimpleTrigger) scheduled.trigger).getRepeatCount());
        }

        @Test
        void noCompletedWorkOrder_returnsWithoutScheduling() throws SchedulerException {
            Schedule schedule = buildSchedule();
            schedule.setRecurrenceBasedOn(RecurrenceBasedOn.COMPLETED_DATE);
            when(workOrderService.findLastByPM(PM_ID, LAST_PM_LIMIT))
                    .thenReturn(pageOf(buildWorkOrder(1L, Status.OPEN, null, new Date())));

            scheduleService.scheduleWorkOrder(schedule);

            verifyNoJobScheduled();
            verify(scheduleRepository, never()).findById(anyLong());
        }

        @Test
        void completedWorkOrder_chainsNextJobFromLatestCompletion() throws SchedulerException {
            Date earlier = Date.from(MONDAY_0915.minus(10, ChronoUnit.DAYS));
            Date later = Date.from(MONDAY_0915.minus(3, ChronoUnit.DAYS));
            when(workOrderService.findLastByPM(PM_ID, LAST_PM_LIMIT)).thenReturn(pageOf(
                    completedWorkOrder(1L, later),
                    completedWorkOrder(2L, earlier)));
            Schedule schedule = buildSchedule();
            schedule.setRecurrenceBasedOn(RecurrenceBasedOn.COMPLETED_DATE);
            schedule.setRecurrenceType(RecurrenceType.DAILY);
            schedule.setFrequency(2);
            when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(schedule));

            scheduleService.scheduleWorkOrder(schedule);

            Date expectedNextRun = Date.from(later.toInstant().plus(2, ChronoUnit.DAYS));
            ScheduledJob chained = jobNamed("wo-job-chained-" + SCHEDULE_ID + "-" + expectedNextRun.getTime());
            assertEquals(expectedNextRun, chained.trigger.getStartTime());
            assertEquals(0, ((SimpleTrigger) chained.trigger).getRepeatCount());
        }

        @Test
        void completedWorkOrder_doesNotScheduleDirectJob() throws SchedulerException {
            when(workOrderService.findLastByPM(PM_ID, LAST_PM_LIMIT))
                    .thenReturn(pageOf(completedWorkOrder(1L, Date.from(MONDAY_0915))));
            Schedule schedule = buildSchedule();
            schedule.setRecurrenceBasedOn(RecurrenceBasedOn.COMPLETED_DATE);
            when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(schedule));

            scheduleService.scheduleWorkOrder(schedule);

            verify(scheduleRepository).findById(SCHEDULE_ID);
            // Only the chained job is created, the direct "wo-job-<id>" one is not.
            assertFalse(scheduledJobs().stream()
                    .anyMatch(scheduled -> scheduled.job.getKey().getName().equals("wo-job-" + SCHEDULE_ID)));
        }
    }

    @Nested
    class ScheduleNextWorkOrderJobAfterCompletion {

        @Test
        void missingSchedule_doesNothing() throws SchedulerException {
            when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.empty());

            scheduleService.scheduleNextWorkOrderJobAfterCompletion(SCHEDULE_ID, new Date());

            verifyNoJobScheduled();
        }

        @Test
        void notBasedOnCompletedDate_doesNothing() throws SchedulerException {
            Schedule schedule = buildSchedule();
            schedule.setRecurrenceBasedOn(RecurrenceBasedOn.SCHEDULED_DATE);
            when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(schedule));

            scheduleService.scheduleNextWorkOrderJobAfterCompletion(SCHEDULE_ID, new Date());

            verifyNoJobScheduled();
        }

        @Test
        void daily_addsFrequencyInDays() throws SchedulerException {
            assertChainedRunDate(RecurrenceType.DAILY, 4, MONDAY_0915.plus(4, ChronoUnit.DAYS));
        }

        @Test
        void weekly_addsFrequencyInWeeks() throws SchedulerException {
            assertChainedRunDate(RecurrenceType.WEEKLY, 3, MONDAY_0915.plus(21, ChronoUnit.DAYS));
        }

        @Test
        void monthly_addsFrequencyInMonths() throws SchedulerException {
            assertChainedRunDate(RecurrenceType.MONTHLY, 2, Instant.parse("2026-05-02T09:15:00Z"));
        }

        @Test
        void yearly_addsFrequencyInYears() throws SchedulerException {
            assertChainedRunDate(RecurrenceType.YEARLY, 2, Instant.parse("2028-03-02T09:15:00Z"));
        }

        @Test
        void schedulerException_isSwallowed() throws SchedulerException {
            Schedule schedule = buildSchedule();
            schedule.setRecurrenceBasedOn(RecurrenceBasedOn.COMPLETED_DATE);
            when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(schedule));
            when(scheduler.scheduleJob(any(JobDetail.class), any(Trigger.class)))
                    .thenThrow(new SchedulerException("quartz down"));

            assertDoesNotThrow(() ->
                    scheduleService.scheduleNextWorkOrderJobAfterCompletion(SCHEDULE_ID, Date.from(MONDAY_0915)));

            verify(scheduler, times(1)).scheduleJob(any(JobDetail.class), any(Trigger.class));
        }

        @Test
        void notificationsEnabled_alsoChainsNotificationJob() throws SchedulerException {
            setDaysBeforeNotification(2);
            Schedule schedule = buildSchedule();
            schedule.setRecurrenceBasedOn(RecurrenceBasedOn.COMPLETED_DATE);
            schedule.setRecurrenceType(RecurrenceType.DAILY);
            schedule.setFrequency(1);
            when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(schedule));

            scheduleService.scheduleNextWorkOrderJobAfterCompletion(SCHEDULE_ID, Date.from(MONDAY_0915));

            Date nextRun = Date.from(MONDAY_0915.plus(1, ChronoUnit.DAYS));
            assertEquals(2, scheduledCount());
            assertEquals(nextRun, jobNamed("wo-job-chained-" + SCHEDULE_ID + "-" + nextRun.getTime())
                    .trigger.getStartTime());
            ScheduledJob notif = jobNamed("notif-job-" + SCHEDULE_ID);
            assertEquals(PreventiveMaintenanceNotificationJob.class, notif.job.getJobClass());
            assertEquals(Date.from(MONDAY_0915.minus(1, ChronoUnit.DAYS)), notif.trigger.getStartTime());
        }

        @Test
        void notificationStartAfterEndsOn_isSkipped() throws SchedulerException {
            setDaysBeforeNotification(5);
            Schedule schedule = buildSchedule();
            schedule.setRecurrenceBasedOn(RecurrenceBasedOn.COMPLETED_DATE);
            schedule.setRecurrenceType(RecurrenceType.DAILY);
            schedule.setFrequency(10);
            schedule.setEndsOn(Date.from(MONDAY_0915.plus(1, ChronoUnit.DAYS)));
            when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(schedule));

            scheduleService.scheduleNextWorkOrderJobAfterCompletion(SCHEDULE_ID, Date.from(MONDAY_0915));

            Date nextRun = Date.from(MONDAY_0915.plus(10, ChronoUnit.DAYS));
            assertEquals(1, scheduledCount());
            assertEquals(nextRun, jobNamed("wo-job-chained-" + SCHEDULE_ID + "-" + nextRun.getTime())
                    .trigger.getStartTime());
        }

        @Test
        void endsOnAfterNextRun_keepsNotificationJob() throws SchedulerException {
            setDaysBeforeNotification(2);
            Schedule schedule = buildSchedule();
            schedule.setRecurrenceBasedOn(RecurrenceBasedOn.COMPLETED_DATE);
            schedule.setRecurrenceType(RecurrenceType.DAILY);
            schedule.setFrequency(1);
            schedule.setEndsOn(Date.from(MONDAY_0915.plus(5, ChronoUnit.DAYS)));
            when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(schedule));

            scheduleService.scheduleNextWorkOrderJobAfterCompletion(SCHEDULE_ID, Date.from(MONDAY_0915));

            assertEquals(2, scheduledCount());
            ScheduledJob notif = jobNamed("notif-job-" + SCHEDULE_ID);
            assertEquals(Date.from(MONDAY_0915.minus(1, ChronoUnit.DAYS)), notif.trigger.getStartTime());
            assertEquals(schedule.getEndsOn(), notif.trigger.getEndTime());
        }

        private void assertChainedRunDate(RecurrenceType recurrenceType, int frequency, Instant expectedNextRun)
                throws SchedulerException {
            Schedule schedule = buildSchedule();
            schedule.setRecurrenceBasedOn(RecurrenceBasedOn.COMPLETED_DATE);
            schedule.setRecurrenceType(recurrenceType);
            schedule.setFrequency(frequency);
            when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(schedule));

            scheduleService.scheduleNextWorkOrderJobAfterCompletion(SCHEDULE_ID, Date.from(MONDAY_0915));

            assertEquals(1, scheduledCount());
            Date nextRun = Date.from(expectedNextRun);
            ScheduledJob chained = jobNamed("wo-job-chained-" + SCHEDULE_ID + "-" + nextRun.getTime());
            assertEquals(nextRun, chained.trigger.getStartTime());
            assertEquals(WorkOrderCreationJob.class, chained.job.getJobClass());
            assertTrue(chained.job.isDurable());
            assertEquals(0, ((SimpleTrigger) chained.trigger).getRepeatCount());
        }
    }

    @Nested
    class ReScheduleWorkOrder {

        @Test
        void stopsExistingJobsBeforeSchedulingAgain() throws SchedulerException {
            Schedule schedule = buildSchedule();

            scheduleService.reScheduleWorkOrder(schedule);

            verify(scheduler).deleteJob(new JobKey("wo-job-" + SCHEDULE_ID, "wo-group"));
            verify(scheduler).deleteJob(new JobKey("notif-job-" + SCHEDULE_ID, "notif-group"));
            assertEquals(1, scheduledCount());
        }

        @Test
        void disabledSchedule_onlyStopsJobs() throws SchedulerException {
            Schedule schedule = buildSchedule();
            schedule.setDisabled(true);

            scheduleService.reScheduleWorkOrder(schedule);

            verify(scheduler).deleteJob(new JobKey("wo-job-" + SCHEDULE_ID, "wo-group"));
            verifyNoJobScheduled();
        }
    }
}
