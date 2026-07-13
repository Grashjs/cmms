package com.grash.service;

import com.grash.dto.SubscriptionPlanPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.SubscriptionPlanMapper;
import com.grash.model.OwnUser;
import com.grash.model.SubscriptionPlan;
import com.grash.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubscriptionPlanService {
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionPlanMapper subscriptionPlanMapper;


    public SubscriptionPlan create(SubscriptionPlan SubscriptionPlan) {
        return subscriptionPlanRepository.save(SubscriptionPlan);
    }

    public SubscriptionPlan update(Long id, SubscriptionPlanPatchDTO subscriptionPlanPatchDTO) {
        if (subscriptionPlanRepository.existsById(id)) {
            SubscriptionPlan savedSubscriptionPlan = subscriptionPlanRepository.findById(id).get();
            return subscriptionPlanRepository.save(subscriptionPlanMapper.updateSubscriptionPlan(savedSubscriptionPlan, subscriptionPlanPatchDTO));
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public Collection<SubscriptionPlan> getAll() {
        return subscriptionPlanRepository.findAll();
    }

    public void delete(Long id) {
        subscriptionPlanRepository.deleteById(id);
    }

    public Optional<SubscriptionPlan> findById(Long id) {
        return subscriptionPlanRepository.findById(id);
    }

    public Optional<SubscriptionPlan> findByCode(String code) {
        return subscriptionPlanRepository.findByCode(code);
    }

    public boolean existByCode(String code) {
        return subscriptionPlanRepository.existsByCode(code);
    }
}
