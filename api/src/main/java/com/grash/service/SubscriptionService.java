package com.grash.service;

import com.grash.dto.SubscriptionPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.SubscriptionMapper;
import com.grash.model.Company;
import com.grash.model.OwnUser;
import com.grash.model.Subscription;
import com.grash.repository.CompanyRepository;
import com.grash.repository.SubscriptionRepository;
import com.grash.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final CompanyService companyService;
    private final SubscriptionPlanService subscriptionPlanService;
    private final SubscriptionMapper subscriptionMapper;
    private final EntityManager em;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    @Transactional
    public Subscription create(Subscription subscription) {
        Subscription savedSubscription = subscriptionRepository.saveAndFlush(subscription);
        em.refresh(savedSubscription);
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
            return updatedSubscription;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public void save(Subscription subscription) {
        subscriptionRepository.save(subscription);
    }

    public Collection<Subscription> getAll() {
        return subscriptionRepository.findAll();
    }

    public void delete(Long id) {
        subscriptionRepository.deleteById(id);
    }

    public Optional<Subscription> findById(Long id) {
        return subscriptionRepository.findById(id);
    }

    public void scheduleEnd(Subscription subscription) {
        boolean shouldSchedule =
                !subscription.getSubscriptionPlan().getCode().equals("FREE") && subscription.getEndsOn() != null;
        if (shouldSchedule) {
            Timer timer = new Timer();
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    resetToFreePlan(subscription);
                }
            };
            timer.schedule(timerTask, subscription.getEndsOn());
        }
    }

    public Optional<Subscription> findByFastSpringId(String id) {
        return subscriptionRepository.findByFastSpringId(id);
    }

    public void resetToFreePlan(Subscription subscription) {
        Optional<Company> optionalCompany = companyRepository.findBySubscription_Id(subscription.getId());
        if (!optionalCompany.isPresent()) return;

        subscription.setActivated(false);
        subscription.setUsersCount(3);
        subscription.setMonthly(true);
        subscription.setFastSpringId(null);
        int currentUsersCount =
                (int) userRepository.findByCompany_Id(optionalCompany.get().getId()).stream().filter(OwnUser::isEnabledInSubscriptionAndPaid).count();
        if (currentUsersCount > subscription.getUsersCount()) {
            subscription.setDowngradeNeeded(true);
        }
        subscription.setCancelled(false);
        subscription.setSubscriptionPlan(subscriptionPlanService.findByCode("FREE").get());
        subscription.setStartsOn(new Date());
        subscription.setEndsOn(null);
        subscriptionRepository.save(subscription);
    }
}
