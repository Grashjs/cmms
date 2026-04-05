package com.grash.controller;

import com.grash.dto.SuperAdminCompanyDTO;
import com.grash.dto.SuperAdminCompanyDetailDTO;
import com.grash.dto.AuthResponse;
import com.grash.model.Company;
import com.grash.model.OwnUser;
import com.grash.model.Subscription;
import com.grash.model.SubscriptionPlan;
import com.grash.repository.SubscriptionRepository;
import com.grash.repository.UserRepository;
import com.grash.security.JwtTokenProvider;
import com.grash.service.CompanyService;
import com.grash.service.SubscriptionPlanService;
import org.springframework.transaction.annotation.Transactional;
import com.grash.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/superadmin")
@PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
@RequiredArgsConstructor
public class SuperAdminController {

    private final CompanyService companyService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final SubscriptionPlanService subscriptionPlanService;
    private final SubscriptionRepository subscriptionRepository;

    @GetMapping("/companies")
    public ResponseEntity<List<SuperAdminCompanyDTO>> getAllCompanies() {
        List<Company> all = new ArrayList<>(companyService.getAll());
        List<SuperAdminCompanyDTO> result = all.stream()
                .map(c -> {
                    SuperAdminCompanyDTO dto = new SuperAdminCompanyDTO();
                    dto.setId(c.getId());
                    dto.setName(c.getName());
                    dto.setEmail(c.getEmail());
                    dto.setUserCount(userRepository.findByCompany_Id(c.getId()).size());
                    return dto;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/companies/{id}")
    public ResponseEntity<SuperAdminCompanyDetailDTO> getCompanyDetail(@PathVariable Long id) {
        Optional<Company> companyOpt = companyService.findById(id);
        if (!companyOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        Company company = companyOpt.get();
        SuperAdminCompanyDetailDTO dto = new SuperAdminCompanyDetailDTO();
        dto.setId(company.getId());
        dto.setName(company.getName());
        dto.setEmail(company.getEmail());
        if (company.getSubscription() != null) {
            Subscription sub = company.getSubscription();
            dto.setUsersLimit(sub.getUsersCount());
            if (sub.getSubscriptionPlan() != null) {
                dto.setSubscriptionPlanId(sub.getSubscriptionPlan().getId());
                dto.setSubscriptionPlanName(sub.getSubscriptionPlan().getName());
            }
        }
        List<OwnUser> users = new ArrayList<>(userRepository.findByCompany_Id(id));
        dto.setUserCount(users.size());
        dto.setUsers(users.stream().map(u -> {
            SuperAdminCompanyDetailDTO.SuperAdminUserDTO userDTO = new SuperAdminCompanyDetailDTO.SuperAdminUserDTO();
            userDTO.setId(u.getId());
            userDTO.setEmail(u.getEmail());
            userDTO.setFirstName(u.getFirstName());
            userDTO.setLastName(u.getLastName());
            userDTO.setRole(u.getRole() != null ? u.getRole().getName() : null);
            return userDTO;
        }).collect(Collectors.toList()));
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/subscription-plans")
    @Transactional
    public ResponseEntity<List<java.util.Map<String, Object>>> getSubscriptionPlans() {
        List<java.util.Map<String, Object>> plans = new ArrayList<>();
        for (SubscriptionPlan plan : subscriptionPlanService.getAll()) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", plan.getId());
            map.put("name", plan.getName());
            map.put("code", plan.getCode());
            plans.add(map);
        }
        return ResponseEntity.ok(plans);
    }

    @PatchMapping("/companies/{id}/plan")
    public ResponseEntity<?> updateCompanyPlan(@PathVariable Long id, @RequestBody PlanUpdateRequest request) {
        Optional<Company> companyOpt = companyService.findById(id);
        if (!companyOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        Optional<SubscriptionPlan> planOpt = subscriptionPlanService.findById(request.getPlanId());
        if (!planOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Subscription plan not found");
        }
        Company company = companyOpt.get();
        Subscription subscription = company.getSubscription();
        if (subscription == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Company has no subscription");
        }
        subscription.setSubscriptionPlan(planOpt.get());
        subscription.setUsersCount(request.getUsersLimit());
        subscriptionRepository.save(subscription);
        return ResponseEntity.ok().build();
    }

    @Data
    public static class PlanUpdateRequest {
        private Long planId;
        private int usersLimit;
    }

    @PostMapping("/switch/{userId}")
    public ResponseEntity<AuthResponse> switchToUser(@PathVariable Long userId) {
        Optional<OwnUser> targetOpt = userService.findById(userId);
        if (!targetOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        OwnUser target = targetOpt.get();
        String token = jwtTokenProvider.createToken(target.getEmail(), List.of(target.getRole().getRoleType()));
        AuthResponse resp = new AuthResponse(token);
        return ResponseEntity.ok(resp);
    }
}
