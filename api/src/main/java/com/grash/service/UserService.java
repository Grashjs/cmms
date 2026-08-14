package com.grash.service;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.advancedsearch.SpecificationBuilder;
import com.grash.dto.AuthTokens;
import com.grash.dto.SignupSuccessResponse;
import com.grash.dto.SuccessResponse;
import com.grash.dto.UserInvitationDTO;
import com.grash.dto.UserPatchDTO;
import com.grash.dto.UserSignupRequest;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.dto.license.LicensingState;
import com.grash.event.CompanyCreatedEvent;
import com.grash.exception.CustomException;
import com.grash.factory.MailServiceFactory;
import com.grash.mapper.UserMapper;
import com.grash.model.*;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleCode;
import com.grash.repository.UserRepository;
import com.grash.repository.VerificationTokenRepository;
import com.grash.security.CustomUserDetail;
import com.grash.security.JwtTokenProvider;
import com.grash.utils.Helper;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

import static com.grash.utils.Consts.usageBasedFreeLimits;


@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EntityManager em;
    private final AuthenticationManager authenticationManager;
    private final MessageSource messageSource;
    private final MailServiceFactory mailServiceFactory;
    private final RoleService roleService;
    private final CompanyService companyService;
    private final CurrencyService currencyService;
    private final UserInvitationService userInvitationService;
    private final VerificationTokenRepository verificationTokenRepository;
    private final SubscriptionPlanService subscriptionPlanService;
    private final SubscriptionService subscriptionService;
    private final UserMapper userMapper;
    private final BrandingService brandingService;
    private final DemoDataService demoDataService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final LicenseService licenseService;
    private final CacheService cacheService;
    private final IntercomService intercomService;
    private final RefreshTokenService refreshTokenService;

    @Value("${api.host}")
    private String PUBLIC_API_URL;
    @Value("${frontend.url}")
    private String frontendUrl;
    @Value("${mail.recipients:#{null}}")
    private String[] recipients;
    @Value("${security.invitation-via-email}")
    private boolean enableInvitationViaEmail;
    @Value("${mail.enable}")
    private boolean enableMails;
    @Value("${cloud-version}")
    private boolean cloudVersion;
    @Value("${allowed-organization-admins}")
    private String[] allowedOrganizationAdmins;

    public AuthTokens signin(String email, String password, String type) {
        try {
            cacheService.evictUserFromCache(email);
            Authentication authentication =
                    authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
            if (authentication.getAuthorities().stream().noneMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_" + type.toUpperCase()))) {
                throw new CustomException("Invalid credentials", HttpStatus.FORBIDDEN);
            }
            Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(email);
            User user = optionalUser.get();
            user.setLastLogin(new Date());
            userRepository.save(user);
            return refreshTokenService.createTokenPair(user);
        } catch (AuthenticationException e) {
            throw new CustomException("Invalid credentials", HttpStatus.FORBIDDEN);
        }
    }


    private void onCompanyAndUserCreation(User user) {
        if (cloudVersion && user.isOwnsCompany()) {
            applicationEventPublisher.publishEvent(new CompanyCreatedEvent(user));
        }
    }

    private SignupSuccessResponse<User> enableAndReturnToken(User user, boolean sendEmailToSuperAdmins,
                                                             UserSignupRequest userSignupRequest) {
        user.setEnabled(true);
        userRepository.save(user);
        if (sendEmailToSuperAdmins)
            sendRegistrationMailToSuperAdmins(user, userSignupRequest);
        onCompanyAndUserCreation(user);
        String accessToken = jwtTokenProvider.createToken(user.getEmail(),
                Collections.singletonList(user.getRole().getRoleType()));
        String refreshToken = refreshTokenService.createRefreshToken(user);
        return new SignupSuccessResponse<>(true, accessToken, user, refreshToken);
    }

    public void checkUsageBasedLimit(int newUsersCount) {
        LicensingState licensingState = licenseService.getLicensingState();
        Integer freeTierThreshold = usageBasedFreeLimits.get(LicenseEntitlement.UNLIMITED_USERS);
        int limit = (licensingState.isHasLicense() ?
                licensingState.getUsersCount() : freeTierThreshold);
        if (!licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_USERS)
                && userRepository.hasMorePaidUsersThan(limit - newUsersCount)
        )
            throw new RuntimeException("Cannot create more users than the " + (licensingState.isHasLicense() ?
                    "paid" : "free") + " license allows: " + limit + ". " +
                    "Refer to" +
                    " https://github.com/Grashjs/cmms/blob/main/dev-docs/Disable%20users.md");
    }

    public SignupSuccessResponse<User> signup(UserSignupRequest userReq) {
        User user = userMapper.toModel(userReq);
        user.setEmail(user.getEmail().toLowerCase());
        if (userRepository.existsByEmailIgnoreCase(user.getEmail())) {
            throw new CustomException("Email is already in use", HttpStatus.UNPROCESSABLE_ENTITY);

        }
        if (allowedOrganizationAdmins != null && userReq.getRole() == null && allowedOrganizationAdmins.length != 0 && Arrays.stream(allowedOrganizationAdmins).noneMatch(allowedOrganizationAdmin -> allowedOrganizationAdmin.equalsIgnoreCase(userReq.getEmail()))) {
            throw new CustomException("You are not allowed to create an account without being invited",
                    HttpStatus.NOT_ACCEPTABLE);
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setUsername(Helper.generateStringId());
        if (user.getRole() == null) {
            //create company with default roles
            if (!licenseService.hasEntitlement(LicenseEntitlement.MULTI_INSTANCE) && companyService.existsAtLeastOneWithMinWorkOrders())
                throw new CustomException("You need a license to create another company", HttpStatus.FORBIDDEN);
            Subscription subscription =
                    Subscription.builder().usersCount(300).monthly(cloudVersion)
                            .startsOn(new Date())
                            .endsOn(cloudVersion ? Helper.incrementDays(new Date(), 15) : null)
                            .subscriptionPlan(subscriptionPlanService.findByCode("BUSINESS").get()).build();
            subscriptionService.create(subscription);
            Company company = new Company(userReq.getCompanyName(), userReq.getEmployeesCount(), subscription);
            company.setDemo(Boolean.TRUE.equals(userReq.getDemo()));
            company.getCompanySettings().getGeneralPreferences().setCurrency(currencyService.findByCode("$").get());
            if (userReq.getLanguage() != null)
                company.getCompanySettings().getGeneralPreferences().setLanguage(userReq.getLanguage());
            if (userReq.getTimeZone() != null)
                company.getCompanySettings().getGeneralPreferences().setTimeZone(userReq.getTimeZone());
            companyService.create(company);
            user.setOwnsCompany(true);
            user.setCompany(company);
            user.setRole(roleService.findDefaultRoleWithCode(RoleCode.ADMIN).get());
            checkUsageBasedLimit(1);
        } else {
            Role role = roleService.findById(user.getRole().getId()).orElseThrow(() -> new CustomException("Role not " +
                    "found", HttpStatus.NOT_ACCEPTABLE));
            if (role.isPaid()) {
                checkUsageBasedLimit(1);
            }
            List<UserInvitation> userInvitations =
                    userInvitationService.findByRoleAndEmail(role.getId(), user.getEmail());
            if (enableInvitationViaEmail && userInvitations.isEmpty()) {
                throw new CustomException("You are not invited to this organization for this role",
                        HttpStatus.NOT_ACCEPTABLE);
            }
            userInvitations.sort(Comparator.comparing(UserInvitation::getCreatedAt).reversed());
            user.setRole(role);
            if (role.getCompanySettings() == null) {
                Optional<User> optionalInviter = findById(userInvitations.get(0).getCreatedBy());
                if (optionalInviter.isEmpty())
                    throw new CustomException("Inviter not found", HttpStatus.NOT_ACCEPTABLE);
                user.setCompany(optionalInviter.get().getCompany());
            } else user.setCompany(role.getCompanySettings().getCompany());
            if (role.isPaid()) {
                int companyUsersCount =
                        (int) findByCompany(user.getCompany().getId()).stream().filter(user1 -> user1.isEnabled() && user1.isEnabledInSubscriptionAndPaid()).count();
                if (companyUsersCount + 1 > user.getCompany().getSubscription().getUsersCount())
                    throw new CustomException("You have reached the maximum number of users for your subscription",
                            HttpStatus.NOT_ACCEPTABLE);
            }
            return enableAndReturnToken(user, true, userReq);
        }
        if (Helper.isLocalhost(PUBLIC_API_URL)) {
            return enableAndReturnToken(user, false, userReq);
        } else {
            if (userReq.getRole() == null) { //send mail
                if (enableInvitationViaEmail) {
                    throwIfEmailNotificationsNotEnabled();
                    String token = UUID.randomUUID().toString();
                    String link = PUBLIC_API_URL + "/auth/activate-account?token=" + token;
                    Map<String, Object> variables = new HashMap<String, Object>() {{
                        put("verifyTokenLink", link);
                    }};
                    user = userRepository.save(user);
                    VerificationToken newUserToken = new VerificationToken(token, user, null);
                    verificationTokenRepository.save(newUserToken);
                    if (!Boolean.TRUE.equals(userReq.getSkipMailSending()))
                        mailServiceFactory.getMailService().sendMessageUsingThymeleafTemplate(new String[]{user.getEmail()},
                                messageSource.getMessage("confirmation_email", null, Helper.getLocale(user)), variables,
                                "signup.html", Helper.getLocale(user), null);
                } else {
                    return enableAndReturnToken(user, true, userReq);
                }
            }
            if (Boolean.TRUE.equals(userReq.getDemo()))
                return enableAndReturnToken(user, false, userReq);
            userRepository.save(user);
            cacheService.putUserInCache(user);
            onCompanyAndUserCreation(user);
            sendRegistrationMailToSuperAdmins(user, userReq);
            return new SignupSuccessResponse<>(true, "Successful registration. Check your mailbox to activate your " +
                    "account", null, null);
        }

    }

    public void delete(String username) {
        userRepository.deleteByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email);
    }

    public Optional<User> findByEmailAndCompany(String email, Long companyId) {
        return userRepository.findByEmailIgnoreCaseAndCompany_Id(email, companyId);
    }

    public Optional<User> findByIdAndCompany(Long id, Long companyId) {
        return userRepository.findByIdAndCompany_Id(id, companyId);
    }

    public User whoami(HttpServletRequest req) {
        return whoami(req, true);
    }

    public User whoami(HttpServletRequest req, boolean cached) {
        String token = jwtTokenProvider.resolveToken(req);
        if (token == null || token.isEmpty()) {
            // API key authentication - get user from SecurityContext
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof CustomUserDetail) {
                return ((CustomUserDetail) auth.getPrincipal()).getUser();
            }
            throw new CustomException("Authentication required", HttpStatus.UNAUTHORIZED);
        }
        String username = jwtTokenProvider.getUsername(token);
        return whoami(username, cached);
    }

    public User whoami(String username, boolean cached) {
        return cached ? findByEmailWithRolesCached(username).get() :
                findByEmail(username).get();
    }

    public Optional<User> findByEmailWithRolesCached(String email) {
        if (email == null || email.trim().isEmpty()) {
            return Optional.empty();
        }
        Optional<User> cachedUser = cacheService.getUserFromCache(email);
        if (cachedUser.isPresent()) return cachedUser;

        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(email.toLowerCase().trim());
        userOptional.ifPresent(cacheService::putUserInCache);

        return userOptional;
    }

    public List<User> getAll() {
        return userRepository.findAll();
    }

    public long count() {
        return userRepository.count();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public void enableUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(email).get();
        enableUser(user);
    }

    public User enableUser(User user) {
        if (user.getRole().isPaid()) {
            checkUsageBasedLimit(1);
            int companyUsersCount =
                    (int) findByCompany(user.getCompany().getId()).stream().filter(user1 -> user1.isEnabled() && user1.isEnabledInSubscriptionAndPaid()).count();
            if (companyUsersCount + 1 > user.getCompany().getSubscription().getUsersCount())
                throw new CustomException("You can't add more users to this company", HttpStatus.NOT_ACCEPTABLE);
        }
        user.setEnabled(true);
        user.setEnabledInSubscription(true);
        userRepository.save(user);
        cacheService.putUserInCache(user);
        return user;
    }

    public SuccessResponse resetPasswordRequest(String email) {
        throwIfEmailNotificationsNotEnabled();
        try {
            email = email.toLowerCase();
            User user = findByEmail(email).get();
            Helper helper = new Helper();
            String password = helper.generateString().replace("-", "").substring(0, 8).toUpperCase();

            String token = UUID.randomUUID().toString();
            Map<String, Object> variables = new HashMap<String, Object>() {{
                put("resetConfirmLink", PUBLIC_API_URL + "/auth/reset-pwd-confirm?token=" + token);
                put("password", password);
            }};
            VerificationToken newUserToken = new VerificationToken(token, user, password);
            verificationTokenRepository.save(newUserToken);
            mailServiceFactory.getMailService().sendMessageUsingThymeleafTemplate(new String[]{email},
                    messageSource.getMessage("password_reset"
                            , new String[]{brandingService.getBrandConfig().getName()}, Helper.getLocale(user)),
                    variables,
                    "reset-password.html", Helper.getLocale(user), null);
        } catch (Exception ignored) {
            //this is done to prevent the user from knowing if email exists or not
        }
        return new SuccessResponse(true, "Password changed successfully");
    }

    public Collection<User> findByCompany(Long id) {
        return userRepository.findByCompany_Id(id);
    }

    public Collection<User> findWorkersByCompany(Long id) {
        return userRepository.findWorkersByCompany(id, Arrays.asList(RoleCode.REQUESTER, RoleCode.VIEW_ONLY));
    }

    public Collection<User> findByLocation(Long id) {
        return userRepository.findByLocation_Id(id);
    }

    private void throwIfEmailNotificationsNotEnabled() {
        if (!enableMails)
            throw new CustomException("Please enable mails and configure SMTP in the environment variables",
                    HttpStatus.NOT_ACCEPTABLE);
    }

    public void invite(String email, Role role, User inviter, Boolean disableSendingMails) {
        if (!userRepository.existsByEmailIgnoreCase(email) && Helper.isValidEmailAddress(email)) {
            if (role.isPaid()) checkUsageBasedLimit(1);
            userInvitationService.create(new UserInvitation(email, role));
            if (!enableInvitationViaEmail || !enableMails) return;
            Map<String, Object> variables = new HashMap<String, Object>() {{
                put("joinLink", frontendUrl + "/account/register?" + "email=" + email + "&role=" + role.getId());
                put("inviter", inviter.getFirstName() + " " + inviter.getLastName());
                put("company", inviter.getCompany().getName());
            }};
            if (!Boolean.TRUE.equals(disableSendingMails))
                mailServiceFactory.getMailService().sendMessageUsingThymeleafTemplate(new String[]{email},
                        messageSource.getMessage(
                                "invitation_to_use", new String[]{brandingService.getBrandConfig().getName()},
                                Helper.getLocale(inviter)), variables, "invite.html",
                        Helper.getLocale(inviter), null);
        } else throw new CustomException("Email already in use", HttpStatus.NOT_ACCEPTABLE);
    }

    @org.springframework.transaction.annotation.Transactional
    public User update(Long id, UserPatchDTO userReq) {
        if (userRepository.existsById(id)) {
            User savedUser = userRepository.findById(id).get();
            if (userReq.getNewPassword() != null) {
                if (userReq.getNewPassword().length() < 8)
                    throw new CustomException("Password must be at least 8 characters", HttpStatus.NOT_ACCEPTABLE);
                if (enableInvitationViaEmail)
                    throw new CustomException("Please tell the user to reset his password", HttpStatus.NOT_FOUND);

                savedUser.setPassword(passwordEncoder.encode(userReq.getNewPassword()));
                savedUser.setSessionInvalidatedAt(new Date());
                refreshTokenService.revokeAllForUser(savedUser);
            }
            User updatedUser = userRepository.saveAndFlush(userMapper.updateUser(savedUser, userReq));
            em.refresh(updatedUser);
            cacheService.putUserInCache(updatedUser);
            return updatedUser;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public User invalidateSessions(User user) {
        user.setSessionInvalidatedAt(new Date());
        User saved = userRepository.save(user);
        cacheService.evictUserFromCache(user.getEmail());
        refreshTokenService.revokeAllForUser(saved);
        return saved;
    }

    public Collection<User> saveAll(Collection<User> users) {
        return userRepository.saveAll(users);
    }

    public Page<User> findBySearchCriteria(SearchCriteria searchCriteria) {
        SpecificationBuilder<User> builder = new SpecificationBuilder<>();
        searchCriteria.getFilterFields().forEach(builder::with);
        Pageable page = PageRequest.of(searchCriteria.getPageNum(), searchCriteria.getPageSize(),
                searchCriteria.getDirection(), searchCriteria.getSortField());
        return userRepository.findAll(builder.build(), page);
    }

    @Async
    void sendRegistrationMailToSuperAdmins(User user, UserSignupRequest userSignupRequest) {
        if (user.getEmail().equals("superadmin@test.com")) return;
        if (user.getCompany() != null && user.getCompany().isDemo()) return;
        if (recipients == null || recipients.length == 0) {
            return;
//            throw new CustomException("MAIL_RECIPIENTS env variable not set", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        try {
            String subject = buildRegistrationEmailSubject(userSignupRequest, brandingService);
            String body = buildRegistrationEmailBody(user, userSignupRequest);
            mailServiceFactory.getMailService().sendHtmlMessage(recipients, subject, body, null);
        } catch (MessagingException | IOException e) {
            e.printStackTrace();
        }
    }

    private String buildRegistrationEmailSubject(UserSignupRequest request, BrandingService brandingService) {
        String brandName = brandingService.getBrandConfig().getShortName();

        if (request.getSubscriptionPlanId() == null) {
            return String.format("New %s registration", brandName);
        }

        return String.format("%s plan %s used", brandName, request.getSubscriptionPlanId());
    }

    private String buildRegistrationEmailBody(User user, UserSignupRequest request) {
        StringBuilder body = new StringBuilder();

        // User basic info
        body.append(String.format("%s %s just created an account%n",
                user.getFirstName(),
                user.getLastName()));

        // Company info
        body.append(String.format("Company: %s (%s employees)%n",
                user.getCompany().getName(),
                request.getEmployeesCount()));

        // Contact info
        body.append(String.format("Email: %s%n", user.getEmail()));
        body.append(String.format("Phone: %s%n", user.getPhone()));

        // Registration type
        if (!user.isOwnsCompany()) {
            body.append("Registration type: After invitation\n");
        }

        // UTM parameters
        appendUtmParameters(body, request);

        return body.toString();
    }

    public SuccessResponse inviteUsers(UserInvitationDTO invitation, User user) {
        if (user.getRole().getCreatePermissions().contains(PermissionEntity.PEOPLE_AND_TEAMS)) {
            int companyUsersCount =
                    (int) findByCompany(user.getCompany().getId()).stream().filter(user1 -> user1.isEnabled() && user1.isEnabledInSubscriptionAndPaid()).count();
            Optional<Role> optionalRole = roleService.findById(invitation.getRole().getId());
            if (optionalRole.isPresent() && optionalRole.get().belongsToCompany(user.getCompany())) {
                if (companyUsersCount + invitation.getEmails().size() <= user.getCompany().getSubscription().getUsersCount() || !optionalRole.get().isPaid()) {
                    invitation.getEmails().forEach(email ->
                            invite(email, optionalRole.get(), user, invitation.getDisableSendingEmail())
                    );

                    // Fire Intercom event for first user invitation
                    if (!user.getCompany().isInvitedUsers() && !invitation.getEmails().isEmpty()) {
                        user.getCompany().setInvitedUsers(true);
                        companyService.update(user.getCompany());
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("invited_count", invitation.getEmails().size());
                        intercomService.createCompanyActivationEvent(
                                "first-users-invited",
                                user.getCompany().getId(),
                                user.getEmail(),
                                metadata
                        );
                    }

                    return new SuccessResponse(true, "Users have been invited");
                } else
                    throw new CustomException("Your current subscription doesn't allow you to invite that many users"
                            , HttpStatus.NOT_ACCEPTABLE);

            } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    public User patchUserRole(Long userId, Long roleId, User requester) {
        Optional<User> optionalUserToPatch = findByIdAndCompany(userId, requester.getCompany().getId());
        Optional<Role> optionalRole = roleService.findById(roleId);
        if (requester.getId().equals(userId)) {
            throw new CustomException("You can't change your own role", HttpStatus.NOT_ACCEPTABLE);
        }
        if (optionalUserToPatch.isPresent() && optionalRole.isPresent() && optionalRole.get().belongsToCompany(requester.getCompany())) {
            User userToPatch = optionalUserToPatch.get();
            if (requester.getRole().getEditOtherPermissions().contains(PermissionEntity.PEOPLE_AND_TEAMS)) {
                int usersCount =
                        (int) findByCompany(requester.getCompany().getId()).stream().filter(User::isEnabledInSubscriptionAndPaid).count();
                if (usersCount <= requester.getCompany().getSubscription().getUsersCount()) {
                    userToPatch.setRole(optionalRole.get());
                    return save(userToPatch);
                } else
                    throw new CustomException("Company subscription users count doesn't allow this operation",
                            HttpStatus.NOT_ACCEPTABLE);
            } else {
                throw new CustomException("You don't have permission", HttpStatus.NOT_ACCEPTABLE);
            }
        } else {
            throw new CustomException("User or role not found", HttpStatus.NOT_FOUND);
        }
    }

    public User disableUser(Long id, User requester) {
        Optional<User> optionalUserToDisable = findByIdAndCompany(id, requester.getCompany().getId());

        if (optionalUserToDisable.isPresent()) {
            User userToDisable = optionalUserToDisable.get();
            if (requester.getRole().getEditOtherPermissions().contains(PermissionEntity.PEOPLE_AND_TEAMS)) {
                userToDisable.setEnabled(false);
                userToDisable.setEnabledInSubscription(false);
                return invalidateSessions(userToDisable);
            } else {
                throw new CustomException("You don't have permission", HttpStatus.NOT_ACCEPTABLE);
            }
        } else {
            throw new CustomException("User or role not found", HttpStatus.NOT_FOUND);
        }
    }

    public User softDeleteUser(Long id, User requester) {
        Optional<User> optionalUserToSoftDelete = findByIdAndCompany(id, requester.getCompany().getId());

        if (optionalUserToSoftDelete.isPresent()) {
            User userToSoftDelete = optionalUserToSoftDelete.get();
            if (requester.getId().equals(id) || requester.getRole().getEditOtherPermissions().contains(PermissionEntity.PEOPLE_AND_TEAMS)) {
                userToSoftDelete.setEnabled(false);
                userToSoftDelete.setEnabledInSubscription(false);
                userToSoftDelete.setEmail(userToSoftDelete.getEmail().concat("_".concat(id.toString())));
                return invalidateSessions(userToSoftDelete);
            } else {
                throw new CustomException("You don't have permission", HttpStatus.NOT_ACCEPTABLE);
            }
        } else {
            throw new CustomException("User not found", HttpStatus.NOT_FOUND);
        }
    }

    private void appendUtmParameters(StringBuilder body, UserSignupRequest request) {
        if (!cloudVersion || request.getUtmParams() == null || !request.getUtmParams().hasAnyParam()) return;
        body.append("\n--- Marketing Attribution ---\n");
        if (request.getUtmParams().getReferrer() != null) {
            body.append(String.format("Referrer: %s%n", request.getUtmParams().getReferrer()));
        }
        if (request.getUtmParams().getUtm_source() != null) {
            body.append(String.format("UTM Source: %s%n", request.getUtmParams().getUtm_source()));
        }
        if (request.getUtmParams().getUtm_medium() != null) {
            body.append(String.format("UTM Medium: %s%n", request.getUtmParams().getUtm_medium()));
        }
        if (request.getUtmParams().getUtm_campaign() != null) {
            body.append(String.format("UTM Campaign: %s%n", request.getUtmParams().getUtm_campaign()));
        }
        if (request.getUtmParams().getUtm_term() != null) {
            body.append(String.format("UTM Term: %s%n", request.getUtmParams().getUtm_term()));
        }
        if (request.getUtmParams().getUtm_content() != null) {
            body.append(String.format("UTM Content: %s%n", request.getUtmParams().getUtm_content()));
        }
        if (request.getUtmParams().getGclid() != null) {
            body.append(String.format("Google Click ID: %s%n", request.getUtmParams().getGclid()));
        }
        if (request.getUtmParams().getFbclid() != null) {
            body.append(String.format("Facebook Click ID: %s%n", request.getUtmParams().getFbclid()));
        }
    }
}

