package com.grash.job;

import com.grash.model.Subscription;
import com.grash.model.SubscriptionPlan;
import com.grash.repository.SubscriptionRepository;
import com.grash.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionEndJobTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private SubscriptionEndJob subscriptionEndJob;

    private Subscription subscription;

    @BeforeEach
    void setUp() {
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .id(1L)
                .name("Pro")
                .features(new HashSet<>())
                .build();
        subscription = Subscription.builder()
                .id(1L)
                .usersCount(10)
                .subscriptionPlan(plan)
                .build();
    }

    private JobExecutionContext contextWithSubscriptionId(long subscriptionId) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("subscriptionId", subscriptionId);
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getMergedJobDataMap()).thenReturn(jobDataMap);
        return context;
    }

    @Test
    void subscriptionNotFound_returnsWithoutResetting() {
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> subscriptionEndJob.executeInternal(contextWithSubscriptionId(1L)));

        verify(subscriptionService, never()).resetToFreePlan(any());
    }

    @Test
    void paddleSubscriptionPresent_returnsWithoutResetting() {
        subscription.setPaddleSubscriptionId("pdl_123");
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));

        assertDoesNotThrow(() -> subscriptionEndJob.executeInternal(contextWithSubscriptionId(1L)));

        verify(subscriptionService, never()).resetToFreePlan(any());
    }

    @Test
    void noPaddleSubscription_resetsToFreePlan() {
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));

        assertDoesNotThrow(() -> subscriptionEndJob.executeInternal(contextWithSubscriptionId(1L)));

        verify(subscriptionService).resetToFreePlan(subscription);
    }

    @Test
    void contextWithoutSubscriptionId_propagatesError() {
        JobDataMap jobDataMap = new JobDataMap();
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getMergedJobDataMap()).thenReturn(jobDataMap);

        assertThrows(ClassCastException.class, () -> subscriptionEndJob.executeInternal(context));

        verify(subscriptionRepository, never()).findById(anyLong());
        verify(subscriptionService, never()).resetToFreePlan(any());
    }
}
