package com.grash.service;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.advancedsearch.SpecificationBuilder;
import com.grash.dto.LdapLoginRequest;
import com.grash.dto.SignupSuccessResponse;
import com.grash.dto.SuccessResponse;
import com.grash.dto.UserPatchDTO;
import com.grash.dto.UserSignupRequest;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.dto.license.LicensingState;
import com.grash.event.CompanyCreatedEvent;
import com.grash.exception.CustomException;
import com.grash.factory.MailServiceFactory;
import com.grash.mapper.UserMapper;
import com.grash.model.*;
import com.grash.model.enums.RoleCode;
import com.grash.repository.UserRepository;
import com.grash.repository.VerificationTokenRepository;
import com.grash.security.CustomUserDetail;
import com.grash.security.JwtTokenProvider;
import com.grash.utils.Helper;
import com.grash.utils.Utils;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

import static com.grash.utils.Consts.usageBasedLicenseLimits;


@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EntityManager em;
    private final AuthenticationManager authenticationManager;
    private final Utils utils;
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

    @Autowired(required = false)
    private LdapAuthenticationProvider ldapAuthenticationProvider;

    @Autowired(required = false)
    private LdapTemplate ldapTemplate;

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

    @Value("${ldap.org-admin:}")
    private String ldapOrgAdmin;

    @Value("${LDAP_ATTR_EMAIL:mail}")
    private String ldapAttrEmail;

    @Value("${LDAP_ATTR_FIRSTNAME:givenName}")
    private String ldapAttrFirstName;

    @Value("${LDAP_ATTR_LASTNAME:sn}")
    private String ldapAttrLastName;

    @Value("${ldap.attributes.username:uid}")
    private String usernameAttr;

    @Value("${ldap.attributes.object-class:inetOrgPerson}")
    private String objectClassAttr;

    @Value("${ldap.sync.enabled:false}")
    private boolean ldapSyncEnabled;
    @Value("${ldap.sync.create:true}")
    private boolean ldapSyncCreate;
    @Value("${ldap.sync.update:true}")
    private boolean ldapSyncUpdate;
    @Value("${ldap.sync.disable:false}")
    private boolean ldapSyncDisable;

    @org.springframework.context.annotation.Profile("!test")
    public void syncLdapUsers() {
        if (!ldapSyncEnabled || ldapTemplate == null || ldapOrgAdmin == null || ldapOrgAdmin.isBlank()) {
            return;
        }

        Company company = companyService.findByOwnerEmailAndOwnsCompany(ldapOrgAdmin)
                .orElse(null);
        if (company == null) {
            return;
        }

        Set<String> ldapUsernames = new HashSet<>(fetchAllLdapUsernames());
        Map<String, User> existingLdapUsersByProviderId = new HashMap<>();
        Collection<User> existingUsers = userRepository.findByCompany_Id(company.getId());

        for (User user : existingUsers) {
            if (!"LDAP".equals(user.getSsoProvider()) || user.getSsoProviderId() == null) {
                continue;
            }
            existingLdapUsersByProviderId.put(user.getSsoProviderId(), user);

            if (!ldapUsernames.contains(user.getSsoProviderId())) {
                if (ldapSyncDisable) {
                    user.setEnabled(false);
                    userRepository.save(user);
                    cacheService.evictUserFromCache(user.getEmail());
                }
            }
        }

        for (String ldapUsername : ldapUsernames) {
            if (existingLdapUsersByProviderId.containsKey(ldapUsername)) {
                User user = existingLdapUsersByProviderId.get(ldapUsername);
                if (ldapSyncUpdate) {
                    Map<String, String> ldapDetails = getLdapUserDetailsByUsername(ldapUsername);
                    boolean updated = false;
                    if (ldapDetails.get("email") != null && !ldapDetails.get("email").equals(user.getEmail())) {
                        user.setEmail(ldapDetails.get("email"));
                        updated = true;
                    }
                    if (ldapDetails.get("firstName") != null && !ldapDetails.get("firstName").equals(user.getFirstName())) {
                        user.setFirstName(ldapDetails.get("firstName"));
                        updated = true;
                    }
                    if (ldapDetails.get("lastName") != null && !ldapDetails.get("lastName").equals(user.getLastName())) {
                        user.setLastName(ldapDetails.get("lastName"));
                        updated = true;
                    }
                    if (updated) {
                        userRepository.save(user);
                        cacheService.evictUserFromCache(user.getEmail());
                    }
                }
                if (!user.isEnabled() && ldapSyncDisable) {
                    user.setEnabled(true);
                    userRepository.save(user);
                }
            } else {
                if (ldapSyncCreate) {
                    Map<String, String> ldapDetails = getLdapUserDetailsByUsername(ldapUsername);
                    User newUser = getNewLdapUser(ldapUsername, company, ldapDetails);
                    userRepository.save(newUser);
                }
            }
        }
    }

    private User getNewLdapUser(String ldapUsername, Company company, Map<String, String> ldapDetails) {
        User user = new User();
        user.setUsername(utils.generateStringId());
        user.setEmail(ldapDetails.get("email"));
        user.setFirstName(ldapDetails.get("firstName"));
        user.setLastName(ldapDetails.get("lastName"));
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setEnabled(true);
        user.setCreatedViaSso(true);
        user.setSsoProvider("LDAP");
        user.setSsoProviderId(ldapUsername);
        user.setLastLogin(null);

        roleService.findDefaultRoles().stream()
                .filter(role -> role.getCode().equals(RoleCode.LIMITED_TECHNICIAN))
                .findFirst().ifPresent(user::setRole);

        user.setCompany(company);
        return user;
    }

    private List<String> fetchAllLdapUsernames() {
        List<String> usernames = new ArrayList<>();
        try {
            EqualsFilter filter = new EqualsFilter("objectClass", objectClassAttr);
            ldapTemplate.search("", filter.encode(),
                    (AttributesMapper<String>) attrs -> {
                        try {
                            return attrs.get(usernameAttr) != null ? attrs.get(usernameAttr).get().toString() : null;
                        } catch (Exception e) {
                            return null;
                        }
                    }
            ).forEach(username -> {
                if (username != null) {
                    usernames.add(username);
                }
            });
        } catch (Exception e) {
            // Return empty list on error
        }
        return usernames;
    }

    private Map<String, String> getLdapUserDetailsByUsername(String username) {
        return extractLdapUserDetails(username);
    }


    public String signin(String email, String password, String type) {
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
            return jwtTokenProvider.createToken(email, Collections.singletonList(user.getRole().getRoleType()));
        } catch (AuthenticationException e) {
            throw new CustomException("Invalid credentials", HttpStatus.FORBIDDEN);
        }
    }

    public String signinLdap(LdapLoginRequest ldapRequest) {
        try {
            if (ldapAuthenticationProvider == null) {
                throw new CustomException("LDAP authentication is not enabled", HttpStatus.FORBIDDEN);
            }
            if (ldapOrgAdmin == null || ldapOrgAdmin.isBlank()) {
                throw new CustomException("LDAP organization admin email is not configured",
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
            if (Helper.isValidEmailAddress(ldapRequest.getUsername())) {
                User user = userRepository.findByEmailIgnoreCase(ldapRequest.getUsername()).orElse(null);
                if (user != null && !"LDAP".equals(user.getSsoProvider())) {
                    return signin(ldapRequest.getUsername(), ldapRequest.getPassword(), "CLIENT");
                }
            }
            String inputUsername = ldapRequest.getUsername();
            String ldapUsername = inputUsername;
            User user = null;

            if (inputUsername.contains("@")) {
                Optional<User> userByEmail = userRepository.findByEmailIgnoreCase(inputUsername);
                if (userByEmail.isPresent() && "LDAP".equals(userByEmail.get().getSsoProvider())) {
                    user = userByEmail.get();
                    ldapUsername = user.getSsoProviderId();
                }
            }

            if (user == null) {
                user = findByLdapId(inputUsername);
                if (user != null) {
                    ldapUsername = user.getSsoProviderId();
                }
            }


            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(ldapUsername, ldapRequest.getPassword());
            ldapAuthenticationProvider.authenticate(authToken);

            String userEmail = user != null ? user.getEmail() : ldapUsername;
            cacheService.evictUserFromCache(userEmail);

            if (user == null) {
                Map<String, String> ldapUserDetails = extractLdapUserDetails(ldapUsername);
                Company company = companyService.findByOwnerEmailAndOwnsCompany(ldapOrgAdmin)
                        .orElseThrow(() -> new CustomException("No company available for LDAP user",
                                HttpStatus.INTERNAL_SERVER_ERROR));
                user = getNewLdapUser(ldapUsername, company, ldapUserDetails);
                user.setLastLogin(new Date());

                Role defaultRole = roleService.findDefaultRoles().stream()
                        .filter(role -> role.getCode().equals(RoleCode.LIMITED_TECHNICIAN))
                        .findFirst()
                        .orElseThrow(() -> new CustomException("Default role not found",
                                HttpStatus.INTERNAL_SERVER_ERROR));
                user.setRole(defaultRole);
            } else {
                user.setLastLogin(new Date());
            }

            user = userRepository.save(user);
            cacheService.putUserInCache(user);
            return jwtTokenProvider.createToken(user.getEmail(),
                    Collections.singletonList(user.getRole().getRoleType()));
        } catch (AuthenticationException e) {
            throw new CustomException("LDAP authentication failed: " + e.getMessage(), HttpStatus.FORBIDDEN);
        }
    }

    private Map<String, String> extractLdapUserDetails(String username) {
        Map<String, String> userDetails = new HashMap<>();

        // ALWAYS derived from username (single source of truth)
        String safeUsername = (username == null || username.isBlank()) ? "unknown" : username;
        String safeEmail = safeUsername.contains("@") ? safeUsername : safeUsername + "@local";

        userDetails.put("email", safeEmail);
        userDetails.put("firstName", safeUsername);
        userDetails.put("lastName", "");
        userDetails.put("source", "fallback");

        try {
            if (ldapTemplate == null || usernameAttr == null || objectClassAttr == null) {
                return userDetails;
            }

            EqualsFilter filter = new EqualsFilter(usernameAttr, safeUsername);

            AndFilter andFilter = new AndFilter();
            andFilter.and(new EqualsFilter("objectClass", objectClassAttr));
            andFilter.and(filter);

            List<Map<String, String>> results = ldapTemplate.search(
                    "",
                    andFilter.encode(),
                    (AttributesMapper<Map<String, String>>) attrs -> {
                        Map<String, String> details = new HashMap<>();

                        try {
                            if (attrs.get(ldapAttrEmail) != null && attrs.get(ldapAttrEmail).get() != null) {
                                details.put("email", attrs.get(ldapAttrEmail).get().toString());
                            }
                            if (attrs.get(ldapAttrFirstName) != null && attrs.get(ldapAttrFirstName).get() != null) {
                                details.put("firstName", attrs.get(ldapAttrFirstName).get().toString());
                            }
                            if (attrs.get(ldapAttrLastName) != null && attrs.get(ldapAttrLastName).get() != null) {
                                details.put("lastName", attrs.get(ldapAttrLastName).get().toString());
                            }
                        } catch (Exception ignored) {
                            // ignore attribute errors
                        }

                        return details;
                    }
            );

            if (results != null && !results.isEmpty() && results.get(0) != null) {
                Map<String, String> ldapData = results.get(0);

                if (ldapData.get("email") != null && !ldapData.get("email").isBlank()) {
                    userDetails.put("email", ldapData.get("email"));
                }

                if (ldapData.get("firstName") != null && !ldapData.get("firstName").isBlank()) {
                    userDetails.put("firstName", ldapData.get("firstName"));
                }

                if (ldapData.get("lastName") != null && !ldapData.get("lastName").isBlank()) {
                    userDetails.put("lastName", ldapData.get("lastName"));
                }

                userDetails.put("source", "ldap");
            }

        } catch (Throwable ignored) {
            // NEVER BREAK LOGIN FLOW
            userDetails.put("source", "fallback");
        }

        return userDetails;
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
        return new SignupSuccessResponse<>(true, jwtTokenProvider.createToken(user.getEmail(),
                Collections.singletonList(user.getRole().getRoleType())), user);
    }

    public void checkUsageBasedLimit(int newUsersCount) {
        LicensingState licensingState = licenseService.getLicensingState();
        if (licensingState.isHasLicense()) {
            if (userRepository.hasMorePaidUsersThan(licensingState.getUsersCount() - newUsersCount))
                throw new RuntimeException("Cannot create more users than the license allows: " + licensingState.getUsersCount() + ". Refer to https://github.com/Grashjs/cmms/blob/main/dev-docs/Disable%20users.md");
        }
        Integer threshold = usageBasedLicenseLimits.get(LicenseEntitlement.UNLIMITED_USERS);
        if (!licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_USERS)
                && userRepository.hasMorePaidUsersThan(threshold - newUsersCount
        ))
            throw new RuntimeException("Cannot create more users than the free license allows: " + threshold + ". " +
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
        user.setUsername(utils.generateStringId());
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
            user.setRole(roleService.findDefaultRoles().stream().filter(role -> role.getName().equals(
                    "Administrator")).findFirst().get());
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
                if (!optionalInviter.isPresent())
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
                    "account", null);
        }

    }

    public void delete(String username) {
        userRepository.deleteByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email);
    }

    public User findByLdapId(String ldapId) {
        return userRepository.findBySsoProviderIdAndSsoProvider(ldapId, "LDAP").orElse(null);
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

    public String refresh(String username) {
        return jwtTokenProvider.createToken(username,
                Arrays.asList(userRepository.findByEmailIgnoreCase(username).get().getRole().getRoleType()));
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
        if (user.getRole().isPaid()) {
            checkUsageBasedLimit(1);
            int companyUsersCount =
                    (int) findByCompany(user.getCompany().getId()).stream().filter(user1 -> user1.isEnabled() && user1.isEnabledInSubscriptionAndPaid()).count();
            if (companyUsersCount + 1 > user.getCompany().getSubscription().getUsersCount())
                throw new CustomException("You can't add more users to this company", HttpStatus.NOT_ACCEPTABLE);
        }
        user.setEnabled(true);
        userRepository.save(user);
        cacheService.putUserInCache(user);
    }

    public SuccessResponse resetPasswordRequest(String email) {
        throwIfEmailNotificationsNotEnabled();
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
                        , new String[]{brandingService.getBrandConfig().getName()}, Helper.getLocale(user)), variables,
                "reset-password.html", Helper.getLocale(user), null);
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

    public Collection<User> saveAll(Collection<User> users) {
        return userRepository.saveAll(users);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailIgnoreCase(email);
    }

    public boolean isUserInCompany(User user, long companyId, boolean optional) {
        if (optional) {
            Optional<User> optionalUser = user == null ? Optional.empty() : findById(user.getId());
            return user == null || (optionalUser.isPresent() && optionalUser.get().getCompany().getId().equals(companyId));
        } else {
            Optional<User> optionalUser = findById(user.getId());
            return optionalUser.isPresent() && optionalUser.get().getCompany().getId().equals(companyId);
        }
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

