package com.grash.service;

import com.grash.dto.SubscriptionPatchDTO;
import com.grash.exception.CustomException;
import com.grash.job.SubscriptionEndJob;
import com.grash.mapper.SubscriptionMapper;
import com.grash.model.Company;
import com.grash.model.OwnUser;
import com.grash.model.Subscription;
import com.grash.repository.CompanyRepository;
import com.grash.repository.SubscriptionRepository;
import com.grash.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final CompanyService companyService;
    private final SubscriptionPlanService subscriptionPlanService;
    private final SubscriptionMapper subscriptionMapper;
    private final EntityManager em;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final Scheduler scheduler;

    @Transactional
    public Subscription create(Subscription subscription) {
        Subscription savedSubscription = subscriptionRepository.saveAndFlush(subscription);
        em.refresh(savedSubscription);
        scheduleEnd(savedSubscription);
        return savedSubscription;
    }

    @Transactional
    public Subscription update(Long id, SubscriptionPatchDTO subscriptionPatchDTO) {
        if (subscriptionRepository.existsById(id)) {
            Subscription savedSubscription = subscriptionRepository.findById(id).get();
            Subscription updatedSubscription =
                    subscriptionRepository.saveAndFlush(subscriptionMapper.updateSubscription(savedSubscription,
                            subscriptionPatchDTO));
            em.refresh(updatedSubscription);
            scheduleEnd(updatedSubscription);
            return updatedSubscription;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public void save(Subscription subscription) {
        subscriptionRepository.save(subscription);
        scheduleEnd(subscription);
    }

    public Collection<Subscription> getAll() {
        return subscriptionRepository.findAll();
    }

    public void delete(Long id) {
        try {
            scheduler.deleteJob(new JobKey("subscription-end-job-" + id, "subscription-group"));
        } catch (SchedulerException e) {
            log.error("Error stopping quartz job for subscription " + id, e);
        }
        subscriptionRepository.deleteById(id);
    }

    public Optional<Subscription> findById(Long id) {
        return subscriptionRepository.findById(id);
    }

    public void scheduleEnd(Subscription subscription) {
        boolean shouldSchedule =
                !subscription.getSubscriptionPlan().getCode().equals("FREE") && subscription.getEndsOn() != null;
        if (shouldSchedule) {
            try {
                JobDetail jobDetail = JobBuilder.newJob(SubscriptionEndJob.class)
                        .withIdentity("subscription-end-job-" + subscription.getId(), "subscription-group")
                        .usingJobData("subscriptionId", subscription.getId())
                        .build();

                Trigger trigger = TriggerBuilder.newTrigger()
                        .withIdentity("subscription-end-trigger-" + subscription.getId(), "subscription-group")
                        .startAt(subscription.getEndsOn())
                        .build();

                if (scheduler.checkExists(jobDetail.getKey())) {
                    scheduler.deleteJob(jobDetail.getKey());
                }
                scheduler.scheduleJob(jobDetail, trigger);
            } catch (SchedulerException e) {
                log.error("Error scheduling subscription end job for subscription " + subscription.getId(), e);
            }
        }
    }

    public Optional<Subscription> findByPaddleSubscriptionId(String id) {
        return subscriptionRepository.findByPaddleSubscriptionId(id);
    }

    public void resetToFreePlan(Subscription subscription) {
        Optional<Company> optionalCompany = companyRepository.findBySubscription_Id(subscription.getId());
        if (!optionalCompany.isPresent()) return;

        subscription.setActivated(false);
        subscription.setUsersCount(3);
        subscription.setMonthly(true);
//        subscription.setPaddleSubscriptionId(null);
        int currentUsersCount =
                (int) userRepository.findByCompany_Id(optionalCompany.get().getId()).stream().filter(OwnUser::isEnabledInSubscriptionAndPaid).count();
        if (currentUsersCount > subscription.getUsersCount()) {
            subscription.setDowngradeNeeded(true);
        }
        subscription.setScheduledChangeType(null);
        subscription.setScheduledChangeDate(null);
        subscription.setSubscriptionPlan(subscriptionPlanService.findByCode("FREE").get());
        subscription.setStartsOn(new Date());
        subscription.setEndsOn(null);
        subscriptionRepository.save(subscription);
    }

    public List<Subscription> findPaidAndEnding() {
        return subscriptionRepository.findPaidAndEnding();
    }
}
