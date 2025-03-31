package com.grash.service;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.advancedsearch.SpecificationBuilder;
import com.grash.dto.SuccessResponse;
import com.grash.dto.UserPatchDTO;
import com.grash.dto.UserSignupRequest;
import com.grash.exception.CustomException;
import com.grash.mapper.UserMapper;
import com.grash.model.*;
import com.grash.model.enums.RoleCode;
import com.grash.repository.UserRepository;
import com.grash.repository.VerificationTokenRepository;
import com.grash.security.JwtTokenProvider;
import com.grash.utils.Helper;
import com.grash.utils.Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.util.*;


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
    private final EmailService2 emailService2;
    private final RoleService roleService;
    private final CompanyService companyService;
    private final CurrencyService currencyService;
    private final UserInvitationService userInvitationService;
    private final VerificationTokenRepository verificationTokenRepository;
    private final SubscriptionPlanService subscriptionPlanService;
    private final SubscriptionService subscriptionService;
    private final UserMapper userMapper;

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


    public String signin(String email, String password, String type) {
        try {
            Authentication authentication =
                    authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
            if (authentication.getAuthorities().stream().noneMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_" + type.toUpperCase()))) {
                throw new CustomException("Invalid credentials", HttpStatus.FORBIDDEN);
            }
            Optional<OwnUser> optionalUser = userRepository.findByEmailIgnoreCase(email);
            OwnUser user = optionalUser.get();
            user.setLastLogin(new Date());
            userRepository.save(user);
            return jwtTokenProvider.createToken(email, Collections.singletonList(user.getRole().getRoleType()));
        } catch (AuthenticationException e) {
            throw new CustomException("Invalid credentials", HttpStatus.FORBIDDEN);
        }
    }

    private SuccessResponse enableAndReturnToken(OwnUser user, boolean sendEmailToSuperAdmins, int employeesCount) {
        user.setEnabled(true);
        userRepository.save(user);
        if (sendEmailToSuperAdmins) sendRegistrationMailToSuperAdmins(user, employeesCount);
        return new SuccessResponse(true, jwtTokenProvider.createToken(user.getEmail(),
                Collections.singletonList(user.getRole().getRoleType())));
    }

    public SuccessResponse signup(UserSignupRequest userReq) {
        OwnUser user = userMapper.toModel(userReq);
        user.setEmail(user.getEmail().toLowerCase());
        if (userRepository.existsByEmailIgnoreCase(user.getEmail())) {
            throw new CustomException("Email is already in use", HttpStatus.UNPROCESSABLE_ENTITY);

        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setUsername(utils.generateStringId());
        if (user.getRole() == null) {
            //create company with default roles
            Subscription subscription = Subscription.builder().usersCount(50).monthly(false)
                    .startsOn(new Date())
//                        .endsOn(Helper.incrementDays(new Date(), 15))
                    .subscriptionPlan(subscriptionPlanService.findByCode("BUSINESS").get()).build();
            subscriptionService.create(subscription);
            Company company = new Company(userReq.getCompanyName(), userReq.getEmployeesCount(), subscription);
            company.getCompanySettings().getGeneralPreferences().setCurrency(currencyService.findByCode("$").get());
            if (userReq.getLanguage() != null)
                company.getCompanySettings().getGeneralPreferences().setLanguage(userReq.getLanguage());
            companyService.create(company);
            user.setOwnsCompany(true);
            user.setCompany(company);
            user.setRole(company.getCompanySettings().getRoleList().stream().filter(role -> role.getName().equals(
                    "Administrator")).findFirst().get());
        } else {
            Optional<Role> optionalRole = roleService.findById(user.getRole().getId());
            if (!optionalRole.isPresent())
                throw new CustomException("Role not found", HttpStatus.NOT_ACCEPTABLE);
            if (enableInvitationViaEmail && userInvitationService.findByRoleAndEmail(optionalRole.get().getId(),
                    user.getEmail()).isEmpty()) {
                throw new CustomException("You are not invited to this organization for this role",
                        HttpStatus.NOT_ACCEPTABLE);
            } else {
                user.setRole(optionalRole.get());
                user.setEnabled(true);
                user.setCompany(optionalRole.get().getCompanySettings().getCompany());
            }
        }
        if (Helper.isLocalhost(PUBLIC_API_URL)) {
            return enableAndReturnToken(user, false, userReq.getEmployeesCount());
        } else {
            if (userReq.getRole() == null) { //send mail
                if (enableInvitationViaEmail) {
                    throwIfEmailNotificationsNotEnabled();
                    String token = UUID.randomUUID().toString();
                    String link = PUBLIC_API_URL + "/auth/activate-account?token=" + token;
                    Map<String, Object> variables = new HashMap<String, Object>() {{
                        put("verifyTokenLink", link);
                        put("featuresLink", frontendUrl + "/#key-features");
                    }};
                    VerificationToken newUserToken = new VerificationToken(token, user);
                    verificationTokenRepository.save(newUserToken);
                    emailService2.sendMessageUsingThymeleafTemplate(new String[]{user.getEmail()},
                            messageSource.getMessage("confirmation_email", null, Helper.getLocale(user)), variables,
                            "signup.html", Helper.getLocale(user));
                } else {
                    return enableAndReturnToken(user, true, userReq.getEmployeesCount());
                }
            }
            userRepository.save(user);
            sendRegistrationMailToSuperAdmins(user, userReq.getEmployeesCount());
            return new SuccessResponse(true, "Successful registration. Check your mailbox to activate your " +
                    "account");
        }

    }

    public void delete(String username) {
        userRepository.deleteByUsername(username);
    }

    public Optional<OwnUser> findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email);
    }

    public Optional<OwnUser> findByEmailAndCompany(String email, Long companyId) {
        return userRepository.findByEmailIgnoreCaseAndCompany_Id(email, companyId);
    }

    public Optional<OwnUser> findByIdAndCompany(Long id, Long companyId) {
        return userRepository.findByIdAndCompany_Id(id, companyId);
    }

    public OwnUser whoami(HttpServletRequest req) {
        return userRepository.findByEmailIgnoreCase(jwtTokenProvider.getUsername(jwtTokenProvider.resolveToken(req))).get();
    }

    public String refresh(String username) {
        return jwtTokenProvider.createToken(username,
                Arrays.asList(userRepository.findByEmailIgnoreCase(username).get().getRole().getRoleType()));
    }

    public List<OwnUser> getAll() {
        return userRepository.findAll();
    }

    public long count() {
        return userRepository.count();
    }

    public Optional<OwnUser> findById(Long id) {
        return userRepository.findById(id);
    }

    public void enableUser(String email) {
        OwnUser user = userRepository.findByEmailIgnoreCase(email).get();
        user.setEnabled(true);
        userRepository.save(user);
    }

    public SuccessResponse resetPassword(String email) {
        throwIfEmailNotificationsNotEnabled();
        email = email.toLowerCase();
        OwnUser user = findByEmail(email).get();
        Helper helper = new Helper();
        String password = helper.generateString().replace("-", "").substring(0, 8).toUpperCase();
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
        String finalEmail = email;
        Map<String, Object> variables = new HashMap<String, Object>() {{
            put("loginLink", frontendUrl + "/account/login?email=" + finalEmail);
            put("featuresLink", frontendUrl + "/#key-features");
            put("password", password);
        }};
        emailService2.sendMessageUsingThymeleafTemplate(new String[]{email}, messageSource.getMessage("password_reset"
                , null, Helper.getLocale(user)), variables, "reset-password.html", Helper.getLocale(user));
        return new SuccessResponse(true, "Password changed successfully");
    }

    public Collection<OwnUser> findByCompany(Long id) {
        return userRepository.findByCompany_Id(id);
    }

    public Collection<OwnUser> findWorkersByCompany(Long id) {
        return userRepository.findWorkersByCompany(id, Arrays.asList(RoleCode.REQUESTER, RoleCode.VIEW_ONLY));
    }

    public Collection<OwnUser> findByLocation(Long id) {
        return userRepository.findByLocation_Id(id);
    }

    private void throwIfEmailNotificationsNotEnabled() {
        if (!enableMails)
            throw new CustomException("Please enable mails and configure SMTP in the environment variables",
                    HttpStatus.NOT_ACCEPTABLE);
    }

    public void invite(String email, Role role, OwnUser inviter) {
        throwIfEmailNotificationsNotEnabled();
        if (!userRepository.existsByEmailIgnoreCase(email) && Helper.isValidEmailAddress(email)) {
            userInvitationService.create(new UserInvitation(email, role));
            Map<String, Object> variables = new HashMap<String, Object>() {{
                put("joinLink", frontendUrl + "/account/register?" + "email=" + email + "&role=" + role.getId());
                put("featuresLink", frontendUrl + "/#key-features");
                put("inviter", inviter.getFirstName() + " " + inviter.getLastName());
                put("company", inviter.getCompany().getName());
            }};
            emailService2.sendMessageUsingThymeleafTemplate(new String[]{email}, messageSource.getMessage(
                            "invitation_to_use", null, Helper.getLocale(inviter)), variables, "invite.html",
                    Helper.getLocale(inviter));
        } else throw new CustomException("Email already in use", HttpStatus.NOT_ACCEPTABLE);
    }

    @org.springframework.transaction.annotation.Transactional
    public OwnUser update(Long id, UserPatchDTO userReq) {
        if (userRepository.existsById(id)) {
            OwnUser savedUser = userRepository.findById(id).get();
            OwnUser updatedUser = userRepository.saveAndFlush(userMapper.updateUser(savedUser, userReq));
            em.refresh(updatedUser);
            return updatedUser;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public OwnUser save(OwnUser user) {
        return userRepository.save(user);
    }

    public Collection<OwnUser> saveAll(Collection<OwnUser> users) {
        return userRepository.saveAll(users);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailIgnoreCase(email);
    }

    public boolean isUserInCompany(OwnUser user, long companyId, boolean optional) {
        if (optional) {
            Optional<OwnUser> optionalUser = user == null ? Optional.empty() : findById(user.getId());
            return user == null || (optionalUser.isPresent() && optionalUser.get().getCompany().getId().equals(companyId));
        } else {
            Optional<OwnUser> optionalUser = findById(user.getId());
            return optionalUser.isPresent() && optionalUser.get().getCompany().getId().equals(companyId);
        }
    }


    public Page<OwnUser> findBySearchCriteria(SearchCriteria searchCriteria) {
        SpecificationBuilder<OwnUser> builder = new SpecificationBuilder<>();
        searchCriteria.getFilterFields().forEach(builder::with);
        Pageable page = PageRequest.of(searchCriteria.getPageNum(), searchCriteria.getPageSize(),
                searchCriteria.getDirection(), "id");
        return userRepository.findAll(builder.build(), page);
    }

    @Async
    void sendRegistrationMailToSuperAdmins(OwnUser user, int employeesCount) {
        if (user.getEmail().equals("superadmin@test.com")) return;
        if (recipients == null || recipients.length == 0) {
            return;
//            throw new CustomException("MAIL_RECIPIENTS env variable not set", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        try {
            emailService2.sendHtmlMessage(recipients, "New Atlas registration",
                    user.getFirstName() + " " + user.getLastName() + " just created an account from company "
                            + user.getCompany().getName() + " with " + employeesCount + " employees.\nEmail: " + user.getEmail()
                            + "\nPhone: " + user.getPhone()
                            + (user.isOwnsCompany() ? "" : " after invitation"));

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
