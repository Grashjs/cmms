package com.grash.util;

import com.grash.dto.RolePermissionRequest;
import com.grash.dto.SignupSuccessResponse;
import com.grash.dto.UserSignupRequest;
import com.grash.model.Role;
import com.grash.model.Subscription;
import com.grash.model.SubscriptionPlan;
import com.grash.model.User;
import com.grash.model.enums.*;
import com.grash.security.CustomUserDetail;
import com.grash.security.JwtTokenProvider;
import com.grash.service.CacheService;
import com.grash.service.RoleService;
import com.grash.service.SubscriptionPlanService;
import com.grash.service.SubscriptionService;
import com.grash.service.UserService;
import com.grash.utils.Helper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class UserTestUtils {

    @Autowired
    private UserService userService;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private RoleService roleService;
    @Autowired
    private CacheService cacheService;
    @Autowired
    private SubscriptionPlanService subscriptionPlanService;
    @Autowired
    private SubscriptionService subscriptionService;

    public User generateUserAndEnable(RoleType roleType) {
        User user = generateDisabledUser(roleType);
        user.setEnabled(true);
        User savedUser = userService.save(user);
        cacheService.putUserInCache(user);
        return savedUser;
    }

    public User generateUserAndEnable() {
        return generateUserAndEnable(RoleType.ROLE_CLIENT);
    }

    public User generateWithoutPrivilege(User inviter, RolePermissionRequest request) {
        Role role =
                roleService.findByCompany(inviter.getCompany().getId()).stream()
                        .filter(r -> !matchesPermissionRequest(r, request))
                        .findFirst().orElseThrow(() ->
                                new IllegalStateException("No role found without the given permissions"));
        return generateWithRole(inviter, role);
    }

    public User generateWithPrivilege(User inviter, RolePermissionRequest request) {
        Role role =
                roleService.findByCompany(inviter.getCompany().getId()).stream()
                        .filter(r -> matchesPermissionRequest(r, request))
                        .min(Comparator.comparingInt(this::countAllPermissions))
                        .orElseThrow(() ->
                                new IllegalStateException("No role found with the required permissions"));
        return generateWithRole(inviter, role);
    }

    private boolean matchesPermissionRequest(Role role, RolePermissionRequest request) {
        return role.getCreatePermissions().containsAll(request.getCreate())
                && role.getViewPermissions().containsAll(request.getView())
                && role.getViewOtherPermissions().containsAll(request.getViewOther())
                && role.getEditOtherPermissions().containsAll(request.getEditOther())
                && role.getDeleteOtherPermissions().containsAll(request.getDeleteOther());
    }

    private int countAllPermissions(Role role) {
        return role.getCreatePermissions().size()
                + role.getViewPermissions().size()
                + role.getViewOtherPermissions().size()
                + role.getEditOtherPermissions().size()
                + role.getDeleteOtherPermissions().size();
    }

    public User generateWithRole(User inviter, Role role) {
        String invitedUserEmail = TestGenerator.generateEmail();
        userService.invite(invitedUserEmail, role, inviter, true);

        UserSignupRequest userSignupRequest = UserSignupRequest.builder()
                .role(role)
                .firstName(TestGenerator.generateString())
                .lastName(TestGenerator.generateString())
                .email(invitedUserEmail)
                .password(TestGenerator.generateString())
                .phone(TestGenerator.generatePhone()).build();
        SignupSuccessResponse<User> signupResponse = userService.signup(userSignupRequest);
        return signupResponse.getUser();
    }


    public UserSignupRequest getRandomSignupRequest(RoleType roleType) {
        return UserSignupRequest.builder()
                .email(TestGenerator.generateEmail().toLowerCase())
                .password(TestGenerator.generateEightCharString())
                .firstName(TestGenerator.generateEightCharString())
                .lastName(TestGenerator.generateEightCharString())
                .phone(TestGenerator.generatePhone())
                .companyName(TestGenerator.generateEightCharString())
                .build();
    }

    public User generateDisabledUser(RoleType roleType) {
        UserSignupRequest signupRequest = getRandomSignupRequest(roleType);
        userService.signup(signupRequest);
        return userService.findByEmail(signupRequest.getEmail()).get();
    }


    public String getToken(User user) {
        return jwtTokenProvider.createToken(user.getEmail(),
                List.of(user.getRole().getRoleType()));
    }

    public void setCurrentUser(User user) {
        CustomUserDetail customUserDetail =
                CustomUserDetail.builder().user(user).build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                customUserDetail,
                null,
                customUserDetail.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public Role getRandomRole(User clinician, Boolean paid) {
        return Helper.getRandomFromCollection(roleService.findByCompany(clinician.getCompany().getId()).stream().filter(role -> {
            if (paid != null) {
                return role.isPaid() == paid;
            }
            return true;
        }).toList()).get();
    }

    public Role getRandomRole(User clinician) {
        return getRandomRole(clinician, null);
    }

    public HttpHeaders getHeaders(User user) {
        String jwtToken = getToken(user);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        return headers;
    }

    public void updateCompanySubscriptionFeatures(User user, Set<PlanFeatures> features) {
        Subscription subscription = user.getCompany().getSubscription();

        SubscriptionPlan lowestMatchingPlan = subscriptionPlanService.getAll().stream()
                .filter(plan -> plan.getFeatures().containsAll(features))
                .min(Comparator.comparingInt(plan -> plan.getFeatures().size()))
                .orElseThrow(() -> new IllegalStateException(
                        "No existing plan found with the required features: " + features));

        subscription.setSubscriptionPlan(lowestMatchingPlan);
        subscriptionService.save(subscription);
    }
}
