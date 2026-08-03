package com.grash.service;

import com.grash.dto.SubscriptionPatchDTO;
import com.grash.exception.CustomException;
import com.grash.factory.MailServiceFactory;
import com.grash.job.SubscriptionEndJob;
import com.grash.mapper.SubscriptionMapper;
import com.grash.model.Company;
import com.grash.model.SubscriptionChangeRequest;
import com.grash.model.User;
import com.grash.model.Subscription;
import com.grash.repository.CompanyRepository;
import com.grash.repository.ScheduleRepository;
import com.grash.repository.SubscriptionChangeRequestRepository;
import com.grash.repository.SubscriptionRepository;
import com.grash.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
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
    private final MailServiceFactory mailServiceFactory;
    private final SubscriptionChangeRequestRepository subscriptionChangeRequestRepository;
    private final BrandingService brandingService;
    @Value("${mail.recipients:#{null}}")
    private String[] recipients;

    public void upgrade(Collection<Long> usersIds, User user) {
        if (user.isOwnsCompany()) {
            int enabledUsersCount =
                    (int) userRepository.findByCompany_Id(user.getCompany().getId()).stream().filter(User::isEnabledInSubscriptionAndPaid).count();
            Subscription subscription = user.getCompany().getSubscription();
            int subscriptionUsersCount = subscription.getUsersCount();
            if (enabledUsersCount + usersIds.size() <= subscriptionUsersCount) {
                Collection<User> users = usersIds.stream().map(userId -> userRepository.findByIdAndCompany_Id(userId,
                        user.getCompany().getId()).get()).collect(Collectors.toList());
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

    public void requestUpgrade(SubscriptionChangeRequest subscriptionChangeRequest, User user) {
        if (user.isOwnsCompany() && !user.getCompany().isDemo()) {
            subscriptionChangeRequestRepository.save(subscriptionChangeRequest);
            try {
                mailServiceFactory.getMailService().sendHtmlMessage(recipients,
                        "New " + brandingService.getBrandConfig().getShortName() +
                                " subscription change request",
                        user.getFirstName() + " " + user.getLastName() + " just requested a subscription change for " +
                                "company " + user.getCompany().getName() + "\nUsers count: " + subscriptionChangeRequest.getUsersCount() + "\nCode: " + subscriptionChangeRequest.getCode() + "\nPeriod: " + (subscriptionChangeRequest.getMonthly() ? "Monthly" : "Annually") + "\nEmail: " + user.getEmail() + "\nPhone: " + user.getPhone(), null);
            } catch (MessagingException | java.io.IOException exception) {
                throw new CustomException(exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }

    public void downgrade(Collection<Long> usersIds, User user) {
        if (user.isOwnsCompany()) {
            int enabledUsersCount =
                    (int) userRepository.findByCompany_Id(user.getCompany().getId()).stream().filter(User::isEnabledInSubscriptionAndPaid).count();
            Subscription subscription = user.getCompany().getSubscription();
            int subscriptionUsersCount = user.getCompany().getSubscription().getUsersCount();
            if (enabledUsersCount - usersIds.size() <= subscriptionUsersCount) {
                Collection<User> users = usersIds.stream().map(userId ->
                                userRepository.findByIdAndCompany_Id(userId, user.getCompany().getId()).get())
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
        if (optionalCompany.isEmpty()) return;

        subscription.setActivated(false);
        subscription.setUsersCount(3);
        subscription.setMonthly(true);
//        subscription.setPaddleSubscriptionId(null);
        Long companyId = optionalCompany.get().getId();
        int currentUsersCount =
                (int) userRepository.findByCompany_Id(companyId).stream().filter(User::isEnabledInSubscriptionAndPaid).count();
        if (currentUsersCount > subscription.getUsersCount()) {
            subscription.setDowngradeNeeded(true);
        }
        subscription.setScheduledChangeType(null);
        subscription.setScheduledChangeDate(null);
        subscription.setSubscriptionPlan(subscriptionPlanService.findByCode("FREE").get());
        scheduleRepository.updateDisabledTrueByCompanyId(companyId);
        subscription.setStartsOn(new Date());
        subscription.setEndsOn(null);
        subscriptionRepository.save(subscription);
    }

}

