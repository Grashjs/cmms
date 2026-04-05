package com.grash.controller;

import com.grash.dto.SuperAdminCompanyDTO;
import com.grash.dto.SuperAdminCompanyDetailDTO;
import com.grash.dto.AuthResponse;
import com.grash.model.Company;
import com.grash.model.CompanyFeatureOverride;
import com.grash.model.OwnUser;
import com.grash.model.Role;
import com.grash.model.Subscription;
import com.grash.model.SubscriptionPlan;
import com.grash.model.enums.PlanFeatures;
import com.grash.model.enums.RoleCode;
import com.grash.model.enums.RoleType;
import com.grash.repository.CompanyFeatureOverrideRepository;
import com.grash.repository.SubscriptionRepository;
import com.grash.repository.UserRepository;
import com.grash.security.JwtTokenProvider;
import com.grash.service.CompanyService;
import com.grash.service.RoleService;
import com.grash.service.SubscriptionPlanService;
import com.grash.service.SubscriptionService;
import com.grash.service.UserService;
import com.grash.utils.Utils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
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
    private final CompanyFeatureOverrideRepository featureOverrideRepository;
    private final RoleService roleService;
    private final SubscriptionService subscriptionService;
    private final PasswordEncoder passwordEncoder;
    private final Utils utils;

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
            dto.setExpiryDate(sub.getExpiryDate());
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
        return ResponseEntity.ok("{\"success\":true}");
    }

    @Data
    public static class PlanUpdateRequest {
        private Long planId;
        private int usersLimit;
    }

    @PatchMapping("/companies/{id}/expiry")
    public ResponseEntity<?> updateCompanyExpiry(@PathVariable Long id, @RequestBody ExpiryUpdateRequest request) {
        Optional<Company> companyOpt = companyService.findById(id);
        if (!companyOpt.isPresent()) return ResponseEntity.notFound().build();
        Company company = companyOpt.get();
        Subscription subscription = company.getSubscription();
        if (subscription == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Company has no subscription");
        }
        subscription.setExpiryDate(request.getExpiryDate());
        subscriptionRepository.save(subscription);
        return ResponseEntity.ok().build();
    }

    @Data
    public static class ExpiryUpdateRequest {
        private java.util.Date expiryDate;
    }

    @GetMapping("/companies/{id}/features")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getCompanyFeatures(@PathVariable Long id) {
        Optional<Company> companyOpt = companyService.findById(id);
        if (!companyOpt.isPresent()) return ResponseEntity.notFound().build();
        Company company = companyOpt.get();
        Set<PlanFeatures> planFeatures = new HashSet<>();
        if (company.getSubscription() != null && company.getSubscription().getSubscriptionPlan() != null) {
            planFeatures.addAll(company.getSubscription().getSubscriptionPlan().getFeatures());
        }
        Map<PlanFeatures, Boolean> overrideMap = new HashMap<>();
        for (CompanyFeatureOverride o : featureOverrideRepository.findByCompanyId(id)) {
            overrideMap.put(o.getFeature(), o.isEnabled());
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (PlanFeatures feature : PlanFeatures.values()) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("feature", feature.name());
            entry.put("inPlan", planFeatures.contains(feature));
            Boolean override = overrideMap.getOrDefault(feature, null);
            entry.put("override", override);
            boolean effective = override != null ? override : planFeatures.contains(feature);
            entry.put("effective", effective);
            result.add(entry);
        }
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/companies/{id}/features")
    @Transactional
    public ResponseEntity<?> updateCompanyFeature(@PathVariable Long id, @RequestBody FeatureOverrideRequest request) {
        Optional<Company> companyOpt = companyService.findById(id);
        if (!companyOpt.isPresent()) return ResponseEntity.notFound().build();
        Company company = companyOpt.get();
        PlanFeatures feature;
        try {
            feature = PlanFeatures.valueOf(request.getFeature());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Unknown feature: " + request.getFeature());
        }
        if (request.getEnabled() == null) {
            featureOverrideRepository.deleteByCompanyIdAndFeature(id, feature);
        } else {
            Optional<CompanyFeatureOverride> existing = featureOverrideRepository.findByCompanyIdAndFeature(id, feature);
            if (existing.isPresent()) {
                existing.get().setEnabled(request.getEnabled());
                featureOverrideRepository.save(existing.get());
            } else {
                featureOverrideRepository.save(CompanyFeatureOverride.builder()
                        .company(company)
                        .feature(feature)
                        .enabled(request.getEnabled())
                        .build());
            }
        }
        return ResponseEntity.ok("{\"success\":true}");
    }

    @Data
    public static class FeatureOverrideRequest {
        private String feature;
        private Boolean enabled; // null = remove override
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

    // ── Company CRUD ──────────────────────────────────────────────────────────

    @PostMapping("/companies")
    @Transactional
    public ResponseEntity<?> createCompany(@RequestBody CreateCompanyRequest request) {
        Long planId = request.getPlanId();
        SubscriptionPlan plan = planId != null
                ? subscriptionPlanService.findById(planId).orElse(null)
                : subscriptionPlanService.findByCode("BUSINESS").orElse(null);
        if (plan == null) return ResponseEntity.badRequest().body("Subscription plan not found");

        Subscription subscription = Subscription.builder()
                .usersCount(request.getUsersLimit() > 0 ? request.getUsersLimit() : 300)
                .monthly(false)
                .activated(true)
                .startsOn(new java.util.Date())
                .subscriptionPlan(plan)
                .build();
        subscriptionService.create(subscription);

        Company company = new Company(request.getName(), 0, subscription);
        company.setEmail(request.getEmail());
        companyService.create(company);

        int userCount = 0;
        if (request.getAdminEmail() != null && !request.getAdminEmail().isBlank()) {
            if (userRepository.existsByEmailIgnoreCase(request.getAdminEmail())) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body("Admin email already in use: " + request.getAdminEmail());
            }
            // Use default Administrator role
            Role adminRole = roleService.findDefaultRoles().stream()
                    .filter(r -> r.getCode() == RoleCode.ADMIN)
                    .findFirst()
                    .orElse(null);
            if (adminRole == null) {
                return ResponseEntity.badRequest().body("Default Administrator role not found");
            }
            OwnUser admin = new OwnUser();
            admin.setEmail(request.getAdminEmail().toLowerCase());
            admin.setFirstName(request.getAdminFirstName() != null ? request.getAdminFirstName() : "");
            admin.setLastName(request.getAdminLastName() != null ? request.getAdminLastName() : "");
            admin.setRole(adminRole);
            admin.setCompany(company);
            admin.setEnabled(true);
            admin.setOwnsCompany(true);
            admin.setUsername(utils.generateStringId());
            String rawPassword = request.getAdminPassword() != null && !request.getAdminPassword().isBlank()
                    ? request.getAdminPassword()
                    : java.util.UUID.randomUUID().toString();
            admin.setPassword(passwordEncoder.encode(rawPassword));
            userRepository.save(admin);
            userCount = 1;
        }

        SuperAdminCompanyDTO dto = new SuperAdminCompanyDTO();
        dto.setId(company.getId());
        dto.setName(company.getName());
        dto.setEmail(company.getEmail());
        dto.setUserCount(userCount);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/companies/{id}")
    @Transactional
    public ResponseEntity<?> deleteCompany(@PathVariable Long id) {
        if (!companyService.findById(id).isPresent()) return ResponseEntity.notFound().build();
        companyService.delete(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/companies/{id}/info")
    @Transactional
    public ResponseEntity<?> updateCompanyInfo(@PathVariable Long id, @RequestBody UpdateCompanyInfoRequest request) {
        Optional<Company> companyOpt = companyService.findById(id);
        if (!companyOpt.isPresent()) return ResponseEntity.notFound().build();
        Company company = companyOpt.get();
        if (request.getName() != null) company.setName(request.getName());
        if (request.getEmail() != null) company.setEmail(request.getEmail());
        companyService.update(company);
        return ResponseEntity.ok().build();
    }

    @Data
    public static class CreateCompanyRequest {
        private String name;
        private String email;
        private Long planId;
        private int usersLimit;
        // Optional admin user
        private String adminFirstName;
        private String adminLastName;
        private String adminEmail;
        private String adminPassword;
    }

    @Data
    public static class UpdateCompanyInfoRequest {
        private String name;
        private String email;
    }

    // ── User management ───────────────────────────────────────────────────────

    @GetMapping("/companies/{id}/roles")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getCompanyRoles(@PathVariable Long id) {
        if (!companyService.findById(id).isPresent()) return ResponseEntity.notFound().build();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Role role : roleService.findByCompany(id)) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", role.getId());
            m.put("name", role.getName());
            m.put("roleType", role.getRoleType().name());
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/companies/{id}/users")
    @Transactional
    public ResponseEntity<?> addUserToCompany(@PathVariable Long id, @RequestBody AddUserRequest request) {
        Optional<Company> companyOpt = companyService.findById(id);
        if (!companyOpt.isPresent()) return ResponseEntity.notFound().build();
        Company company = companyOpt.get();

        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("Email already in use");
        }
        Optional<Role> roleOpt = roleService.findById(request.getRoleId());
        if (!roleOpt.isPresent()) return ResponseEntity.badRequest().body("Role not found");

        OwnUser user = new OwnUser();
        user.setEmail(request.getEmail().toLowerCase());
        user.setFirstName(request.getFirstName() != null ? request.getFirstName() : "");
        user.setLastName(request.getLastName() != null ? request.getLastName() : "");
        user.setRole(roleOpt.get());
        user.setCompany(company);
        user.setEnabled(true);
        user.setUsername(utils.generateStringId());
        String rawPwd = request.getPassword() != null && !request.getPassword().isBlank()
                ? request.getPassword()
                : java.util.UUID.randomUUID().toString();
        user.setPassword(passwordEncoder.encode(rawPwd));
        OwnUser saved = userRepository.save(user);

        SuperAdminCompanyDetailDTO.SuperAdminUserDTO dto = new SuperAdminCompanyDetailDTO.SuperAdminUserDTO();
        dto.setId(saved.getId());
        dto.setEmail(saved.getEmail());
        dto.setFirstName(saved.getFirstName());
        dto.setLastName(saved.getLastName());
        dto.setRole(saved.getRole().getName());
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/companies/{id}/users/{userId}")
    @Transactional
    public ResponseEntity<?> removeUserFromCompany(@PathVariable Long id, @PathVariable Long userId) {
        Optional<OwnUser> userOpt = userRepository.findByIdAndCompany_Id(userId, id);
        if (!userOpt.isPresent()) return ResponseEntity.notFound().build();
        userRepository.delete(userOpt.get());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/companies/{id}/users/{userId}/role")
    @Transactional
    public ResponseEntity<?> changeUserRole(@PathVariable Long id, @PathVariable Long userId,
                                            @RequestBody ChangeRoleRequest request) {
        Optional<OwnUser> userOpt = userRepository.findByIdAndCompany_Id(userId, id);
        if (!userOpt.isPresent()) return ResponseEntity.notFound().build();
        Optional<Role> roleOpt = roleService.findById(request.getRoleId());
        if (!roleOpt.isPresent()) return ResponseEntity.badRequest().body("Role not found");
        OwnUser user = userOpt.get();
        user.setRole(roleOpt.get());
        userRepository.save(user);
        return ResponseEntity.ok().build();
    }

    @Data
    public static class AddUserRequest {
        private String email;
        private String firstName;
        private String lastName;
        private Long roleId;
        private String password; // optional; random UUID used if blank
    }

    @Data
    public static class ChangeRoleRequest {
        private Long roleId;
    }

    @PatchMapping("/companies/{id}/users/{userId}/password")
    @Transactional
    public ResponseEntity<?> changeUserPassword(@PathVariable Long id, @PathVariable Long userId,
                                                @RequestBody ChangePasswordRequest request) {
        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            return ResponseEntity.badRequest().body("Password cannot be empty");
        }
        Optional<OwnUser> userOpt = userRepository.findByIdAndCompany_Id(userId, id);
        if (!userOpt.isPresent()) return ResponseEntity.notFound().build();
        OwnUser user = userOpt.get();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return ResponseEntity.ok().build();
    }

    @Data
    public static class ChangePasswordRequest {
        private String newPassword;
    }
}
