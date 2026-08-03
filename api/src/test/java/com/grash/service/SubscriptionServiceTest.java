package com.grash.service;

import com.grash.exception.CustomException;
import com.grash.mapper.SubscriptionMapper;
import com.grash.model.Company;
import com.grash.model.Subscription;
import com.grash.model.SubscriptionPlan;
import com.grash.model.User;
import com.grash.model.enums.RoleCode;
import com.grash.model.enums.RoleType;
import com.grash.model.Role;
import com.grash.repository.CompanyRepository;
import com.grash.repository.ScheduleRepository;
import com.grash.repository.SubscriptionRepository;
import com.grash.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @InjectMocks
    private SubscriptionService subscriptionService;

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private SubscriptionPlanService subscriptionPlanService;
    @Mock
    private SubscriptionMapper subscriptionMapper;
    @Mock
    private EntityManager em;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private Scheduler scheduler;
    @Mock
    private ScheduleRepository scheduleRepository;

    private Company company;
    private User owner;
    private Subscription subscription;
    private SubscriptionPlan businessPlan;
    private SubscriptionPlan freePlan;
    private Role paidRole;

    @BeforeEach
    void setUp() {
        businessPlan = SubscriptionPlan.builder()
                .id(1L)
                .name("Business")
                .code("BUSINESS")
                .features(Set.of())
                .build();
        freePlan = SubscriptionPlan.builder()
                .id(2L)
                .name("Free")
                .code("FREE")
                .features(Set.of())
                .build();

        subscription = Subscription.builder()
                .id(1L)
                .usersCount(5)
                .monthly(true)
                .activated(true)
                .subscriptionPlan(businessPlan)
                .build();

        company = new Company("TestCo", 10, subscription);
        company.setId(1L);
        company.setDemo(false);

        paidRole = Role.builder()
                .id(1L)
                .name("Administrator")
                .roleType(RoleType.ROLE_CLIENT)
                .code(RoleCode.ADMIN)
                .paid(true)
                .build();

        owner = buildUser(1L, true, true, true);
    }

    private User buildUser(Long id, boolean enabled, boolean enabledInSubscription, boolean ownsCompany) {
        User u = new User();
        u.setId(id);
        u.setFirstName("U" + id);
        u.setLastName("L" + id);
        u.setEmail("user" + id + "@test.com");
        u.setUsername("user" + id);
        u.setPassword("encoded");
        u.setRole(paidRole);
        u.setCompany(company);
        u.setEnabled(enabled);
        u.setEnabledInSubscription(enabledInSubscription);
        u.setOwnsCompany(ownsCompany);
        return u;
    }

    private void stubEnabledUsers(User... users) {
        when(userRepository.findByCompany_Id(1L)).thenReturn(List.of(users));
    }

    @Nested
    class Upgrade {

        @Test
        void nonOwner_throwsForbidden() {
            User nonOwner = buildUser(2L, true, true, false);

            CustomException ex = assertThrows(CustomException.class,
                    () -> subscriptionService.upgrade(List.of(3L), nonOwner));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
            verify(userRepository, never()).saveAll(anyCollection());
        }

        @Test
        void overSubscriptionLimit_throwsNotAcceptable() {
            stubEnabledUsers(owner, buildUser(2L, true, true, false), buildUser(3L, true, true, false),
                    buildUser(4L, true, true, false), buildUser(5L, true, true, false));

            CustomException ex = assertThrows(CustomException.class,
                    () -> subscriptionService.upgrade(List.of(10L, 11L), owner));

            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
            verify(userRepository, never()).saveAll(anyCollection());
        }

        @Test
        void alreadyEnabledUser_throwsNotAcceptable() {
            User alreadyEnabled = buildUser(10L, true, true, false);
            stubEnabledUsers(owner, alreadyEnabled);
            when(userRepository.findByIdAndCompany_Id(10L, 1L)).thenReturn(Optional.of(alreadyEnabled));

            CustomException ex = assertThrows(CustomException.class,
                    () -> subscriptionService.upgrade(List.of(10L), owner));

            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
            verify(userRepository, never()).saveAll(anyCollection());
        }

        @Test
        void validUpgrade_enablesUsersAndSaves() {
            User target = buildUser(10L, false, false, false);
            stubEnabledUsers(owner);
            when(userRepository.findByIdAndCompany_Id(10L, 1L)).thenReturn(Optional.of(target));
            when(userRepository.saveAll(anyCollection())).thenAnswer(inv -> inv.getArgument(0));

            subscriptionService.upgrade(List.of(10L), owner);

            assertTrue(target.isEnabled());
            assertTrue(target.isEnabledInSubscription());
            verify(userRepository).saveAll(argThat(users -> ((Iterable<?>) users).iterator().next() == target));
            verify(subscriptionRepository).save(subscription);
            assertFalse(subscription.isUpgradeNeeded());
        }

        @Test
        void userFromOtherCompany_throwsNotFound() {
            stubEnabledUsers(owner);
            when(userRepository.findByIdAndCompany_Id(10L, 1L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> subscriptionService.upgrade(List.of(10L), owner));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
            verify(userRepository, never()).saveAll(anyCollection());
        }

        @Test
        void emptyUserIds_savesWithoutEnablingAnyone() {
            stubEnabledUsers(owner);

            subscriptionService.upgrade(List.of(), owner);

            verify(userRepository).saveAll(argThat(users -> !((java.util.Collection<?>) users).iterator().hasNext()));
            verify(subscriptionRepository).save(subscription);
        }
    }


    @Nested
    class Downgrade {

        @Test
        void nonOwner_throwsForbidden() {
            User nonOwner = buildUser(2L, true, true, false);

            CustomException ex = assertThrows(CustomException.class,
                    () -> subscriptionService.downgrade(List.of(3L), nonOwner));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
            verify(userRepository, never()).saveAll(anyCollection());
        }

        @Test
        void alreadyDisabledUser_throwsNotAcceptable() {
            User target = buildUser(10L, false, false, false);
            stubEnabledUsers(owner);
            when(userRepository.findByIdAndCompany_Id(10L, 1L)).thenReturn(Optional.of(target));

            CustomException ex = assertThrows(CustomException.class,
                    () -> subscriptionService.downgrade(List.of(10L), owner));

            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
            verify(userRepository, never()).saveAll(anyCollection());
        }

        @Test
        void validDowngrade_disablesUsersAndSaves() {
            User target = buildUser(10L, true, true, false);
            stubEnabledUsers(owner, target);
            when(userRepository.findByIdAndCompany_Id(10L, 1L)).thenReturn(Optional.of(target));
            when(userRepository.saveAll(anyCollection())).thenAnswer(inv -> inv.getArgument(0));

            subscriptionService.downgrade(List.of(10L), owner);

            assertFalse(target.isEnabled());
            assertFalse(target.isEnabledInSubscription());
            verify(userRepository).saveAll(argThat(users -> ((Iterable<?>) users).iterator().next() == target));
            verify(subscriptionRepository).save(subscription);
            assertFalse(subscription.isDowngradeNeeded());
        }

        @Test
        void ownerInList_isFilteredOut() {
            User target = buildUser(10L, true, true, false);
            stubEnabledUsers(owner, target);
            when(userRepository.findByIdAndCompany_Id(1L, 1L)).thenReturn(Optional.of(owner));
            when(userRepository.findByIdAndCompany_Id(10L, 1L)).thenReturn(Optional.of(target));
            when(userRepository.saveAll(anyCollection())).thenAnswer(inv -> inv.getArgument(0));

            subscriptionService.downgrade(List.of(1L, 10L), owner);

            assertTrue(owner.isEnabled());
            assertTrue(owner.isEnabledInSubscription());
            assertFalse(target.isEnabled());
            verify(userRepository).saveAll(argThat(users -> {
                List<?> list = new java.util.ArrayList<>((java.util.Collection<?>) users);
                return list.size() == 1 && list.get(0) == target;
            }));
        }

        @Test
        void userFromOtherCompany_throwsNotFound() {
            stubEnabledUsers(owner);
            when(userRepository.findByIdAndCompany_Id(10L, 1L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> subscriptionService.downgrade(List.of(10L), owner));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
            verify(userRepository, never()).saveAll(anyCollection());
        }
    }

    @Nested
    class Create {

        @Test
        void savesFlushesRefreshesAndSchedulesEnd() throws SchedulerException {
            subscription.setEndsOn(new Date(System.currentTimeMillis() + 60_000));
            when(subscriptionRepository.saveAndFlush(subscription)).thenReturn(subscription);
            when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);

            Subscription result = subscriptionService.create(subscription);

            assertEquals(subscription, result);
            verify(em).refresh(subscription);
            verify(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
        }
    }

    @Nested
    class Save {

        @Test
        void savesAndSchedulesEnd() throws SchedulerException {
            subscription.setEndsOn(new Date(System.currentTimeMillis() + 60_000));
            when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);

            subscriptionService.save(subscription);

            verify(subscriptionRepository).save(subscription);
            verify(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
        }
    }

    @Nested
    class Delete {

        @Test
        void deletesJobAndSubscription() throws SchedulerException {
            subscriptionService.delete(1L);

            verify(scheduler).deleteJob(new JobKey("subscription-end-job-1", "subscription-group"));
            verify(subscriptionRepository).deleteById(1L);
        }

        @Test
        void schedulerFailure_stillDeletesSubscription() throws SchedulerException {
            doThrow(new SchedulerException("boom")).when(scheduler).deleteJob(any(JobKey.class));

            subscriptionService.delete(1L);

            verify(subscriptionRepository).deleteById(1L);
        }
    }

    @Nested
    class FindById {

        @Test
        void existingId_returnsSubscription() {
            when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));

            Optional<Subscription> result = subscriptionService.findById(1L);

            assertTrue(result.isPresent());
            assertEquals(subscription, result.get());
        }

        @Test
        void nonExistingId_returnsEmpty() {
            when(subscriptionRepository.findById(99L)).thenReturn(Optional.empty());

            Optional<Subscription> result = subscriptionService.findById(99L);

            assertFalse(result.isPresent());
        }
    }

    @Nested
    class ScheduleEnd {

        @Test
        void freePlan_doesNotScheduleAndRemovesExistingJob() throws SchedulerException {
            subscription.setSubscriptionPlan(freePlan);
            subscription.setEndsOn(new Date());
            when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);

            subscriptionService.scheduleEnd(subscription);

            verify(scheduler).deleteJob(new JobKey("subscription-end-job-1", "subscription-group"));
            verify(scheduler, never()).scheduleJob(any(JobDetail.class), any(Trigger.class));
        }

        @Test
        void freePlan_noExistingJob_doesNothing() throws SchedulerException {
            subscription.setSubscriptionPlan(freePlan);
            subscription.setEndsOn(new Date());
            when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);

            subscriptionService.scheduleEnd(subscription);

            verify(scheduler, never()).deleteJob(any(JobKey.class));
            verify(scheduler, never()).scheduleJob(any(JobDetail.class), any(Trigger.class));
        }

        @Test
        void nullEndsOn_doesNotScheduleAndRemovesExistingJob() throws SchedulerException {
            subscription.setEndsOn(null);
            when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);

            subscriptionService.scheduleEnd(subscription);

            verify(scheduler).deleteJob(new JobKey("subscription-end-job-1", "subscription-group"));
            verify(scheduler, never()).scheduleJob(any(JobDetail.class), any(Trigger.class));
        }

        @Test
        void paddleSubscription_doesNotSchedule() throws SchedulerException {
            subscription.setEndsOn(new Date());
            subscription.setPaddleSubscriptionId("pdl_123");
            when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);

            subscriptionService.scheduleEnd(subscription);

            verify(scheduler, never()).deleteJob(any(JobKey.class));
            verify(scheduler, never()).scheduleJob(any(JobDetail.class), any(Trigger.class));
        }

        @Test
        void schedulable_schedulesJob() throws SchedulerException {
            subscription.setEndsOn(new Date(System.currentTimeMillis() + 60_000));
            when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);

            subscriptionService.scheduleEnd(subscription);

            ArgumentCaptor<JobDetail> jobCaptor = ArgumentCaptor.forClass(JobDetail.class);
            ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
            verify(scheduler).scheduleJob(jobCaptor.capture(), triggerCaptor.capture());
            assertEquals(new JobKey("subscription-end-job-1", "subscription-group"), jobCaptor.getValue().getKey());
            assertNotNull(triggerCaptor.getValue());
        }

        @Test
        void existingJob_deletedBeforeReschedule() throws SchedulerException {
            subscription.setEndsOn(new Date(System.currentTimeMillis() + 60_000));
            when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);

            subscriptionService.scheduleEnd(subscription);

            verify(scheduler).deleteJob(new JobKey("subscription-end-job-1", "subscription-group"));
            verify(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
        }

        @Test
        void schedulerException_swallowed() throws SchedulerException {
            subscription.setEndsOn(new Date(System.currentTimeMillis() + 60_000));
            when(scheduler.checkExists(any(JobKey.class))).thenThrow(new SchedulerException("boom"));

            assertDoesNotThrow(() -> subscriptionService.scheduleEnd(subscription));

            verify(scheduler, never()).scheduleJob(any(JobDetail.class), any(Trigger.class));
        }
    }

    @Nested
    class FindByPaddleSubscriptionId {

        @Test
        void returnsSubscription() {
            when(subscriptionRepository.findByPaddleSubscriptionId("pdl_1")).thenReturn(Optional.of(subscription));

            Optional<Subscription> result = subscriptionService.findByPaddleSubscriptionId("pdl_1");

            assertTrue(result.isPresent());
            assertEquals(subscription, result.get());
        }
    }

    @Nested
    class ResetToFreePlan {

        @Test
        void noCompanyForSubscription_returnsEarly() {
            when(companyRepository.findBySubscription_Id(1L)).thenReturn(Optional.empty());

            subscriptionService.resetToFreePlan(subscription);

            verify(subscriptionRepository, never()).save(any());
        }

        @Test
        void resetsSubscriptionToFreePlan() {
            User active = buildUser(2L, true, true, false);
            when(companyRepository.findBySubscription_Id(1L)).thenReturn(Optional.of(company));
            when(userRepository.findByCompany_Id(1L)).thenReturn(List.of(owner, active));
            when(subscriptionPlanService.findByCode("FREE")).thenReturn(Optional.of(freePlan));

            subscriptionService.resetToFreePlan(subscription);

            assertFalse(subscription.isActivated());
            assertEquals(3, subscription.getUsersCount());
            assertTrue(subscription.isMonthly());
            assertEquals(freePlan, subscription.getSubscriptionPlan());
            assertNull(subscription.getEndsOn());
            assertFalse(subscription.isDowngradeNeeded());
            verify(scheduleRepository).updateDisabledTrueByCompanyId(1L);
            verify(subscriptionRepository).save(subscription);
        }

        @Test
        void currentUsersOverLimit_setsDowngradeNeeded() {
            when(companyRepository.findBySubscription_Id(1L)).thenReturn(Optional.of(company));
            when(userRepository.findByCompany_Id(1L)).thenReturn(List.of(owner,
                    buildUser(2L, true, true, false),
                    buildUser(3L, true, true, false),
                    buildUser(4L, true, true, false)));
            when(subscriptionPlanService.findByCode("FREE")).thenReturn(Optional.of(freePlan));

            subscriptionService.resetToFreePlan(subscription);

            assertTrue(subscription.isDowngradeNeeded());
        }

        @Test
        void freePlanNotFound_throws() {
            when(companyRepository.findBySubscription_Id(1L)).thenReturn(Optional.of(company));
            when(userRepository.findByCompany_Id(1L)).thenReturn(List.of(owner));
            when(subscriptionPlanService.findByCode("FREE")).thenReturn(Optional.empty());

            assertThrows(NoSuchElementException.class, () -> subscriptionService.resetToFreePlan(subscription));
        }
    }
}
