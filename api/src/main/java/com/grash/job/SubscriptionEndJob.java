package com.grash.job;

import com.grash.model.Subscription;
import com.grash.repository.SubscriptionRepository;
import com.grash.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionEndJob extends QuartzJobBean {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;

    @Override
    @Transactional
    public void executeInternal(JobExecutionContext context) throws JobExecutionException {
        Long subscriptionId = context.getMergedJobDataMap().getLong("subscriptionId");

        Subscription subscription = subscriptionRepository.findById(subscriptionId).orElse(null);
        if (subscription == null) {
            log.warn("Subscription not found for ID: {}", subscriptionId);
            return;
        }

        log.info("Resetting subscription to FREE plan for subscription ID: {}", subscriptionId);
        subscriptionService.resetToFreePlan(subscription);
    }
}
