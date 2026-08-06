package com.grash.service;

// Modified by the Fierabrás CMMS fork, 2026-08 — AGPLv3: commercial
// subscription degradation neutralized via a reusable AGPL policy. See NOTICE.md.

import com.grash.dto.SubscriptionPatchDTO;
import com.grash.exception.CustomException;
import com.grash.job.SubscriptionEndJob;
import com.grash.mapper.SubscriptionMapper;
import com.grash.model.Company;
import com.grash.model.User;
import com.grash.model.Subscription;
import com.grash.repository.CompanyRepository;
import com.grash.repository.ScheduleRepository;
import com.grash.repository.SubscriptionRepository;
import com.grash.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanService subscriptionPlanService;
    private final SubscriptionMapper subscriptionMapper;
    private final EntityManager em;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final Scheduler scheduler;
    private final ScheduleRepository scheduleRepository;

    public void upgrade(Collection<Long> usersIds, User user) {
        if (user.isOwnsCompany()) {
            int enabledUsersCount =
                    (int) userRepository.findByCompany_Id(user.getCompany().getId()).stream().filter(User::isEnabledInSubscriptionAndPaid).count();
            Subscription subscription = user.getCompany().getSubscription();
            int subscriptionUsersCount = subscription.getUsersCount();
            if (enabledUsersCount + usersIds.size() <= subscriptionUsersCount) {
                Collection<User> users = usersIds.stream().map(userId -> findUserInCompany(userId,
                                user.getCompany().getId()))
                        .collect(Collectors.toList());
                if (users.stream().noneMatch(User::isEnabledInSubscription)) {
                    users.forEach(user1 -> {
                        user1.setEnabled(true);
                        user1.setEnabledInSubscription(true);
                    });
                    userRepository.saveAll(users);
                    subscription.setUpgradeNeeded(false);
                    save(subscription);
                } else throw new CustomException("There are some already enabled users", HttpStatus.NOT_ACCEPTABLE);
            } else
                throw new CustomException("The subscription users count doesn't permit this operation",
                        HttpStatus.NOT_ACCEPTABLE);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    public void downgrade(Collection<Long> usersIds, User user) {
        if (user.isOwnsCompany()) {
            int enabledUsersCount =
                    (int) userRepository.findByCompany_Id(user.getCompany().getId()).stream().filter(User::isEnabledInSubscriptionAndPaid).count();
            Subscription subscription = user.getCompany().getSubscription();
            int subscriptionUsersCount = user.getCompany().getSubscription().getUsersCount();
            if (enabledUsersCount - usersIds.size() <= subscriptionUsersCount) {
                Collection<User> users = usersIds.stream().map(userId -> findUserInCompany(userId,
                                user.getCompany().getId()))
                        .filter(user1 -> !user1.isOwnsCompany()).collect(Collectors.toList());
                if (users.stream().allMatch(User::isEnabledInSubscription)) {
                    users.forEach(user1 -> {
                        user1.setEnabled(false);
                        user1.setEnabledInSubscription(false);
                    });
                    userRepository.saveAll(users);
                    subscription.setDowngradeNeeded(false);
                    save(subscription);
                } else throw new CustomException("There are some already disabled users", HttpStatus.NOT_ACCEPTABLE);
            } else
                throw new CustomException("The subscription users count doesn't permit this operation",
                        HttpStatus.NOT_ACCEPTABLE);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }

    public Subscription create(Subscription subscription) {
        Subscription savedSubscription = subscriptionRepository.saveAndFlush(subscription);
        em.refresh(savedSubscription);
        scheduleEnd(savedSubscription);
        return savedSubscription;
    }


    public void save(Subscription subscription) {
        subscriptionRepository.save(subscription);
        scheduleEnd(subscription);
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
                !subscription.getSubscriptionPlan().getCode().equals("FREE") && subscription.getEndsOn() != null && subscription.getPaddleSubscriptionId() == null;
        JobKey jobKey = new JobKey("subscription-end-job-" + subscription.getId(), "subscription-group");
        if (shouldSchedule) {
            try {
                JobDetail jobDetail = JobBuilder.newJob(SubscriptionEndJob.class)
                        .withIdentity(jobKey)
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
        } else {
            try {
                if (scheduler.checkExists(jobKey)) {
                    scheduler.deleteJob(jobKey);
                }
            } catch (SchedulerException e) {
                log.error("Error removing subscription end job for subscription " + subscription.getId(), e);
            }
        }
    }

    private User findUserInCompany(Long userId, Long companyId) {
        return userRepository.findByIdAndCompany_Id(userId, companyId)
                .orElseThrow(() -> new CustomException("User not found in company", HttpStatus.NOT_FOUND));
    }

    public Optional<Subscription> findByPaddleSubscriptionId(String id) {
        return subscriptionRepository.findByPaddleSubscriptionId(id);
    }

    public void resetToFreePlan(Subscription subscription) {
        // AGPLv3 fork: do not degrade to FREE. Apply the AGPL subscription policy,
        // which removes seat limits and degradation schedules while preserving
        // historical identifiers, the historical plan, and user-disabled schedules.
        // Callers (SubscriptionEndJob, Paddle webhook handlers) thus neutralize
        // any future degradation without destroying audit data.
        applyAgplSubscriptionPolicy(subscription);
    }

    /**
     * AGPLv3 fork: neutralizes commercial seat limits and subscription
     * degradation for an existing Subscription, without destroying historical
     * or external identifiers (paddleSubscriptionId is preserved), without
     * changing the historical plan, and without disabling schedules that the
     * user may have disabled voluntarily. Idempotent: only persists when a
     * field actually changes. Removing any pending Quartz degradation job is
     * delegated to scheduleEnd() (endsOn=null => job removed).
     */
    public void applyAgplSubscriptionPolicy(Subscription subscription) {
        boolean changed = false;
        if (subscription.getUsersCount() != Integer.MAX_VALUE) {
            subscription.setUsersCount(Integer.MAX_VALUE);
            changed = true;
        }
        if (subscription.getEndsOn() != null) {
            subscription.setEndsOn(null);
            changed = true;
        }
        if (subscription.getScheduledChangeType() != null) {
            subscription.setScheduledChangeType(null);
            changed = true;
        }
        if (subscription.getScheduledChangeDate() != null) {
            subscription.setScheduledChangeDate(null);
            changed = true;
        }
        // upgradeNeeded/downgradeNeeded are purely commercial seat-flow flags (no
        // backend functional gating); reset them so no seat banner is shown.
        if (subscription.isUpgradeNeeded()) {
            subscription.setUpgradeNeeded(false);
            changed = true;
        }
        if (subscription.isDowngradeNeeded()) {
            subscription.setDowngradeNeeded(false);
            changed = true;
        }
        if (changed) {
            subscriptionRepository.save(subscription);
            scheduleEnd(subscription); // endsOn=null => removes any pending Quartz degradation job
        }
    }

    /**
     * AGPLv3 fork: apply the AGPL subscription policy to every existing
     * subscription at startup. Converges after the first pass (no writes on
     * subsequent boots).
     */
    public void normalizeExistingSubscriptions() {
        for (Subscription subscription : subscriptionRepository.findAll()) {
            applyAgplSubscriptionPolicy(subscription);
        }
    }

}

