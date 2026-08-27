package com.grash.service;

import com.grash.dto.AuthTokens;
import com.grash.dto.SignupSuccessResponse;
import com.grash.dto.SuccessResponse;
import com.grash.dto.UserInvitationDTO;
import com.grash.dto.UserPatchDTO;
import com.grash.dto.UserSignupRequest;
import com.grash.dto.UtmParams;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.dto.license.LicensingState;
import com.grash.event.CompanyCreatedEvent;
import com.grash.exception.CustomException;
import com.grash.factory.MailServiceFactory;
import com.grash.mapper.UserMapper;
import com.grash.dto.BrandConfig;
import com.grash.model.*;
import com.grash.model.enums.Language;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleCode;
import com.grash.model.enums.RoleType;
import com.grash.repository.UserRepository;
import com.grash.repository.VerificationTokenRepository;
import com.grash.security.CustomUserDetail;
import com.grash.security.JwtTokenProvider;
import com.grash.utils.Helper;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private EntityManager em;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private MessageSource messageSource;
    @Mock
    private MailServiceFactory mailServiceFactory;
    @Mock
    private RoleService roleService;
    @Mock
    private CompanyService companyService;
    @Mock
    private CurrencyService currencyService;
    @Mock
    private UserInvitationService userInvitationService;
    @Mock
    private VerificationTokenRepository verificationTokenRepository;
    @Mock
    private SubscriptionPlanService subscriptionPlanService;
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private BrandingService brandingService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private LicenseService licenseService;
    @Mock
    private CacheService cacheService;
    @Mock
    private MailService mailService;
    @Mock
    private IntercomService intercomService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private ApiKeyService apiKeyService;

    private Company company;
    private User user;
    private Role role;
    private Subscription subscription;
    private SubscriptionPlan subscriptionPlan;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "PUBLIC_API_URL", "http://localhost:8080");
        ReflectionTestUtils.setField(userService, "frontendUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(userService, "recipients", new String[]{"admin@test.com"});
        ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", false);
        ReflectionTestUtils.setField(userService, "enableMails", true);
        ReflectionTestUtils.setField(userService, "cloudVersion", false);
        ReflectionTestUtils.setField(userService, "allowedOrganizationAdmins", new String[]{});

        subscriptionPlan = SubscriptionPlan.builder()
                .id(1L)
                .name("Business")
                .features(new HashSet<>())
                .build();
        subscription = Subscription.builder()
                .id(1L)
                .usersCount(300)
                .monthly(false)
                .subscriptionPlan(subscriptionPlan)
                .build();
        company = new Company("TestCo", 10, subscription);
        company.setId(1L);
        company.setDemo(false);

        role = Role.builder()
                .id(1L)
                .name("Administrator")
                .roleType(RoleType.ROLE_CLIENT)
                .code(RoleCode.ADMIN)
                .paid(true)
                .build();

        user = new User();
        user.setId(1L);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@test.com");
        user.setUsername("johndoe");
        user.setPassword("encoded-password");
        user.setRole(role);
        user.setCompany(company);
        user.setEnabled(true);
        user.setEnabledInSubscription(true);
        user.setOwnsCompany(false);
        user.setPhone("123-456-7890");
    }

    private User buildUser(Long id, String email) {
        User u = new User();
        u.setId(id);
        u.setFirstName("U" + id);
        u.setLastName("L" + id);
        u.setEmail(email);
        u.setUsername("user" + id);
        u.setPassword("encoded");
        u.setRole(role);
        u.setCompany(company);
        u.setEnabled(true);
        u.setEnabledInSubscription(true);
        return u;
    }

    @Nested
    class Signin {

        @Test
        void validCredentials_returnsToken() {
            Authentication authentication = mock(Authentication.class);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getAuthorities())
                    .thenAnswer(inv -> List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));
            when(userRepository.findByEmailIgnoreCase("john@test.com")).thenReturn(Optional.of(user));
            when(refreshTokenService.createTokenPair(user))
                    .thenReturn(new AuthTokens("jwt-token", "refresh-token", new Date()));

            AuthTokens tokens = userService.signin("john@test.com", "password", "CLIENT");

            assertEquals("jwt-token", tokens.getAccessToken());
            assertEquals("refresh-token", tokens.getRefreshToken());
            verify(cacheService).evictUserFromCache("john@test.com");
            verify(userRepository).save(user);
            verify(refreshTokenService).createTokenPair(user);
            assertNotNull(user.getLastLogin());
        }

        @Test
        void wrongRoleType_throwsForbidden() {
            Authentication authentication = mock(Authentication.class);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getAuthorities())
                    .thenAnswer(inv -> List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));

            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.signin("john@test.com", "password", "TECHNICIAN"));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void authenticationException_throwsForbidden() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new org.springframework.security.authentication.BadCredentialsException("bad"));

            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.signin("john@test.com", "password", "CLIENT"));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }
    }

    @Nested
    class Signup {

        private UserSignupRequest signupRequest;
        private User mappedUser;

        @BeforeEach
        void init() {
            signupRequest = UserSignupRequest.builder()
                    .email("new@test.com")
                    .password("password123")
                    .firstName("New")
                    .lastName("User")
                    .phone("555-0100")
                    .companyName("NewCo")
                    .employeesCount(5)
                    .build();
            mappedUser = new User();
            mappedUser.setEmail("new@test.com");
            mappedUser.setPassword("password123");
            mappedUser.setFirstName("New");
            mappedUser.setLastName("User");
            mappedUser.setPhone("555-0100");
            lenient().when(brandingService.getBrandConfig()).thenReturn(new BrandConfig());
            lenient().when(mailServiceFactory.getMailService()).thenReturn(mailService);
        }

        @Test
        void existingEmail_throws() {
            when(userMapper.toModel(signupRequest)).thenReturn(mappedUser);
            when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(true);

            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.signup(signupRequest));
            assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getHttpStatus());
        }

        @Test
        void allowedOrganizationAdmins_blocksNonAdmin() {
            ReflectionTestUtils.setField(userService, "allowedOrganizationAdmins",
                    new String[]{"allowed@test.com"});
            when(userMapper.toModel(signupRequest)).thenReturn(mappedUser);
            when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(false);

            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.signup(signupRequest));
            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        void newCompanyOnLocalhost_withLanguageAndTimeZone() {
            signupRequest.setLanguage(Language.FR);
            signupRequest.setTimeZone("Europe/Paris");
            when(userMapper.toModel(signupRequest)).thenReturn(mappedUser);
            when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");
            when(licenseService.hasEntitlement(LicenseEntitlement.MULTI_INSTANCE)).thenReturn(true);
            when(subscriptionPlanService.findByCode("BUSINESS"))
                    .thenReturn(Optional.of(subscriptionPlan));
            when(licenseService.getLicensingState()).thenReturn(
                    LicensingState.builder().hasLicense(false).usersCount(0).build());
            com.grash.model.Currency currency = new com.grash.model.Currency();
            when(currencyService.findByCode("$")).thenReturn(Optional.of(currency));
            when(roleService.findDefaultRoleWithCode(RoleCode.ADMIN)).thenReturn(Optional.of(role));
            when(jwtTokenProvider.createToken(eq("new@test.com"), anyList()))
                    .thenReturn("signup-token");

            SignupSuccessResponse<User> response = userService.signup(signupRequest);

            assertTrue(response.isSuccess());
        }

        @Test
        void newCompanyOnLocalhost_success() {
            ReflectionTestUtils.setField(userService, "cloudVersion", false);
            when(userMapper.toModel(signupRequest)).thenReturn(mappedUser);
            when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");
            when(licenseService.hasEntitlement(LicenseEntitlement.MULTI_INSTANCE)).thenReturn(true);
            when(subscriptionPlanService.findByCode("BUSINESS"))
                    .thenReturn(Optional.of(subscriptionPlan));
            when(licenseService.getLicensingState()).thenReturn(
                    LicensingState.builder().hasLicense(false).usersCount(0).build());
            com.grash.model.Currency currency = new com.grash.model.Currency();
            currency.setCode("$");
            when(currencyService.findByCode("$")).thenReturn(Optional.of(currency));
            when(roleService.findDefaultRoleWithCode(RoleCode.ADMIN)).thenReturn(Optional.of(role));
            when(jwtTokenProvider.createToken(eq("new@test.com"), anyList()))
                    .thenReturn("signup-token");

            SignupSuccessResponse<User> response = userService.signup(signupRequest);

            assertTrue(response.isSuccess());
            assertNotNull(response.getUser());
            assertTrue(response.getUser().isEnabled());
            assertTrue(response.getUser().isOwnsCompany());
            verify(companyService).create(any(Company.class));
            verify(subscriptionService).create(any(Subscription.class));
        }

        @Test
        void newCompanyNotLocalhostWithoutInvitationEmail_success() {
            ReflectionTestUtils.setField(userService, "PUBLIC_API_URL", "https://app.test.com");
            ReflectionTestUtils.setField(userService, "cloudVersion", false);
            ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", false);
            when(userMapper.toModel(signupRequest)).thenReturn(mappedUser);
            when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");
            when(licenseService.hasEntitlement(LicenseEntitlement.MULTI_INSTANCE)).thenReturn(true);
            when(subscriptionPlanService.findByCode("BUSINESS"))
                    .thenReturn(Optional.of(subscriptionPlan));
            when(licenseService.getLicensingState()).thenReturn(
                    LicensingState.builder().hasLicense(false).usersCount(0).build());
            com.grash.model.Currency currency = new com.grash.model.Currency();
            currency.setCode("$");
            when(currencyService.findByCode("$")).thenReturn(Optional.of(currency));
            when(roleService.findDefaultRoleWithCode(RoleCode.ADMIN)).thenReturn(Optional.of(role));
            when(jwtTokenProvider.createToken(eq("new@test.com"), anyList()))
                    .thenReturn("signup-token");

            SignupSuccessResponse<User> response = userService.signup(signupRequest);

            assertTrue(response.isSuccess());
            assertTrue(response.getUser().isEnabled());
        }

        @Test
        void invitedUser_inviterNotFound_throws() {
            Role invitedRole = Role.builder()
                    .id(2L).name("Technician").roleType(RoleType.ROLE_CLIENT)
                    .code(RoleCode.TECHNICIAN).paid(true).build();
            signupRequest.setRole(invitedRole);
            mappedUser.setRole(invitedRole);
            UserInvitation invitation = new UserInvitation("new@test.com", invitedRole);
            invitation.setId(1L);
            invitation.setCreatedBy(99L);

            when(userMapper.toModel(signupRequest)).thenReturn(mappedUser);
            when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");
            when(roleService.findById(2L)).thenReturn(Optional.of(invitedRole));
            when(userInvitationService.findByRoleAndEmail(2L, "new@test.com"))
                    .thenReturn(new ArrayList<>(List.of(invitation)));
            when(userRepository.findById(99L)).thenReturn(Optional.empty());
            when(licenseService.getLicensingState()).thenReturn(
                    LicensingState.builder().hasLicense(false).usersCount(0).build());
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_USERS)).thenReturn(true);

            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.signup(signupRequest));
            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        void invitedNonPaidRole_success() {
            Role invitedRole = Role.builder()
                    .id(2L).name("Viewer").roleType(RoleType.ROLE_CLIENT)
                    .code(RoleCode.VIEW_ONLY).paid(false).build();
            signupRequest.setRole(invitedRole);
            mappedUser.setRole(invitedRole);
            UserInvitation invitation = new UserInvitation("new@test.com", invitedRole);
            invitation.setId(1L);
            invitation.setCreatedBy(1L);

            when(userMapper.toModel(signupRequest)).thenReturn(mappedUser);
            when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");
            when(roleService.findById(2L)).thenReturn(Optional.of(invitedRole));
            when(userInvitationService.findByRoleAndEmail(2L, "new@test.com"))
                    .thenReturn(new ArrayList<>(List.of(invitation)));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(jwtTokenProvider.createToken(eq("new@test.com"), anyList()))
                    .thenReturn("signup-token");

            SignupSuccessResponse<User> response = userService.signup(signupRequest);

            assertTrue(response.isSuccess());
            assertNotNull(response.getUser());
        }

        @Test
        void invitedUserWithPaidRole_success() {
            Role invitedRole = Role.builder()
                    .id(2L).name("Technician").roleType(RoleType.ROLE_CLIENT)
                    .code(RoleCode.TECHNICIAN).paid(true).build();
            signupRequest.setRole(invitedRole);
            mappedUser.setRole(invitedRole);
            UserInvitation invitation = new UserInvitation("new@test.com", invitedRole);
            invitation.setId(1L);
            invitation.setCreatedBy(1L);

            when(userMapper.toModel(signupRequest)).thenReturn(mappedUser);
            when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");
            when(roleService.findById(2L)).thenReturn(Optional.of(invitedRole));
            when(userInvitationService.findByRoleAndEmail(2L, "new@test.com"))
                    .thenReturn(new ArrayList<>(List.of(invitation)));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(licenseService.getLicensingState()).thenReturn(
                    LicensingState.builder().hasLicense(false).usersCount(0).build());
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_USERS)).thenReturn(true);
            when(userRepository.findByCompany_Id(1L)).thenReturn(List.of());
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(jwtTokenProvider.createToken(eq("new@test.com"), anyList()))
                    .thenReturn("signup-token");

            SignupSuccessResponse<User> response = userService.signup(signupRequest);

            assertTrue(response.isSuccess());
            assertNotNull(response.getUser());
        }

        @Test
        void invitedRoleNotFound_throws() {
            Role invitedRole = Role.builder()
                    .id(99L).name("Unknown").roleType(RoleType.ROLE_CLIENT)
                    .code(RoleCode.USER_CREATED).paid(false).build();
            signupRequest.setRole(invitedRole);
            mappedUser.setRole(invitedRole);

            when(userMapper.toModel(signupRequest)).thenReturn(mappedUser);
            when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");
            when(roleService.findById(99L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.signup(signupRequest));
            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        void newCompany_noMultiInstanceEntitlement_throws() {
            when(userMapper.toModel(signupRequest)).thenReturn(mappedUser);
            when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");
            when(licenseService.hasEntitlement(LicenseEntitlement.MULTI_INSTANCE)).thenReturn(false);
            when(companyService.existsAtLeastOneWithMinWorkOrders()).thenReturn(true);

            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.signup(signupRequest));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void newCompanyOnLocalhost_cloudVersionTrue() {
            ReflectionTestUtils.setField(userService, "PUBLIC_API_URL", "http://localhost:8080");
            ReflectionTestUtils.setField(userService, "cloudVersion", true);
            when(userMapper.toModel(signupRequest)).thenReturn(mappedUser);
            when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");
            when(licenseService.hasEntitlement(LicenseEntitlement.MULTI_INSTANCE)).thenReturn(true);
            when(subscriptionPlanService.findByCode("BUSINESS"))
                    .thenReturn(Optional.of(subscriptionPlan));
            when(licenseService.getLicensingState()).thenReturn(
                    LicensingState.builder().hasLicense(false).usersCount(0).build());
            com.grash.model.Currency currency = new com.grash.model.Currency();
            currency.setCode("$");
            when(currencyService.findByCode("$")).thenReturn(Optional.of(currency));
            when(roleService.findDefaultRoleWithCode(RoleCode.ADMIN)).thenReturn(Optional.of(role));
            when(jwtTokenProvider.createToken(eq("new@test.com"), anyList()))
                    .thenReturn("signup-token");

            SignupSuccessResponse<User> response = userService.signup(signupRequest);

            assertTrue(response.isSuccess());
            verify(applicationEventPublisher).publishEvent(any(CompanyCreatedEvent.class));
        }

        @Test
        void newCompanyInProduction_cloudVersionTrue_publishesEvent() {
            ReflectionTestUtils.setField(userService, "PUBLIC_API_URL", "https://app.test.com");
            ReflectionTestUtils.setField(userService, "cloudVersion", true);
            ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", false);
            when(userMapper.toModel(signupRequest)).thenReturn(mappedUser);
            when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");
            when(licenseService.hasEntitlement(LicenseEntitlement.MULTI_INSTANCE)).thenReturn(true);
            when(subscriptionPlanService.findByCode("BUSINESS"))
                    .thenReturn(Optional.of(subscriptionPlan));
            when(licenseService.getLicensingState()).thenReturn(
                    LicensingState.builder().hasLicense(false).usersCount(0).build());
            com.grash.model.Currency currency = new com.grash.model.Currency();
            currency.setCode("$");
            when(currencyService.findByCode("$")).thenReturn(Optional.of(currency));
            when(roleService.findDefaultRoleWithCode(RoleCode.ADMIN)).thenReturn(Optional.of(role));
            when(jwtTokenProvider.createToken(eq("new@test.com"), anyList()))
                    .thenReturn("signup-token");

            ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
            SignupSuccessResponse<User> response = userService.signup(signupRequest);

            assertTrue(response.isSuccess());
            assertNotNull(response.getUser());
            assertTrue(response.getUser().isOwnsCompany());
            assertTrue(response.getUser().isEnabled());
            verify(applicationEventPublisher).publishEvent(any(CompanyCreatedEvent.class));
            verify(subscriptionService).create(subscriptionCaptor.capture());
            assertTrue(subscriptionCaptor.getValue().isMonthly());
            assertNotNull(subscriptionCaptor.getValue().getEndsOn());
        }

        @Test
        void newCompanyOnLocalhost_cloudVersion_subscriptionIsMonthlyWithTrialEnd() {
            ReflectionTestUtils.setField(userService, "PUBLIC_API_URL", "http://localhost:8080");
            ReflectionTestUtils.setField(userService, "cloudVersion", true);
            when(userMapper.toModel(signupRequest)).thenReturn(mappedUser);
            when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");
            when(licenseService.hasEntitlement(LicenseEntitlement.MULTI_INSTANCE)).thenReturn(true);
            when(subscriptionPlanService.findByCode("BUSINESS"))
                    .thenReturn(Optional.of(subscriptionPlan));
            when(licenseService.getLicensingState()).thenReturn(
                    LicensingState.builder().hasLicense(false).usersCount(0).build());
            com.grash.model.Currency currency = new com.grash.model.Currency();
            currency.setCode("$");
            when(currencyService.findByCode("$")).thenReturn(Optional.of(currency));
            when(roleService.findDefaultRoleWithCode(RoleCode.ADMIN)).thenReturn(Optional.of(role));
            when(jwtTokenProvider.createToken(eq("new@test.com"), anyList()))
                    .thenReturn("signup-token");

            ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
            userService.signup(signupRequest);

            verify(subscriptionService).create(subscriptionCaptor.capture());
            Subscription created = subscriptionCaptor.getValue();
            assertTrue(created.isMonthly());
            assertNotNull(created.getEndsOn());
            Date expectedEnd = Helper.incrementDays(new Date(), 15);
            assertTrue(Math.abs(created.getEndsOn().getTime() - expectedEnd.getTime()) < 60_000);
        }

        @Test
        void newCompanyOnLocalhost_nonCloud_subscriptionNotMonthlyWithoutEnd() {
            ReflectionTestUtils.setField(userService, "PUBLIC_API_URL", "http://localhost:8080");
            ReflectionTestUtils.setField(userService, "cloudVersion", false);
            when(userMapper.toModel(signupRequest)).thenReturn(mappedUser);
            when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");
            when(licenseService.hasEntitlement(LicenseEntitlement.MULTI_INSTANCE)).thenReturn(true);
            when(subscriptionPlanService.findByCode("BUSINESS"))
                    .thenReturn(Optional.of(subscriptionPlan));
            when(licenseService.getLicensingState()).thenReturn(
                    LicensingState.builder().hasLicense(false).usersCount(0).build());
            com.grash.model.Currency currency = new com.grash.model.Currency();
            currency.setCode("$");
            when(currencyService.findByCode("$")).thenReturn(Optional.of(currency));
            when(roleService.findDefaultRoleWithCode(RoleCode.ADMIN)).thenReturn(Optional.of(role));
            when(jwtTokenProvider.createToken(eq("new@test.com"), anyList()))
                    .thenReturn("signup-token");

            ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
            userService.signup(signupRequest);

            verify(subscriptionService).create(subscriptionCaptor.capture());
            Subscription created = subscriptionCaptor.getValue();
            assertFalse(created.isMonthly());
            assertNull(created.getEndsOn());
        }

        @Test
        void invitedRole_noInvitationsFound_throws() {
            Role invitedRole = Role.builder()
                    .id(2L).name("Technician").roleType(RoleType.ROLE_CLIENT)
                    .code(RoleCode.TECHNICIAN).paid(false).build();
            signupRequest.setRole(invitedRole);
            mappedUser.setRole(invitedRole);
            ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", true);

            when(userMapper.toModel(signupRequest)).thenReturn(mappedUser);
            when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");
            when(roleService.findById(2L)).thenReturn(Optional.of(invitedRole));
            when(userInvitationService.findByRoleAndEmail(2L, "new@test.com"))
                    .thenReturn(new ArrayList<>());

            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.signup(signupRequest));
            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        void invitedRole_withCompanySettings_success() {
            Role invitedRole = Role.builder()
                    .id(2L).name("Technician").roleType(RoleType.ROLE_CLIENT)
                    .code(RoleCode.TECHNICIAN).paid(true).build();
            CompanySettings companySettings = new CompanySettings();
            companySettings.setCompany(company);
            invitedRole.setCompanySettings(companySettings);
            signupRequest.setRole(invitedRole);
            mappedUser.setRole(invitedRole);
            UserInvitation invitation = new UserInvitation("new@test.com", invitedRole);
            invitation.setId(1L);
            invitation.setCreatedBy(1L);

            when(userMapper.toModel(signupRequest)).thenReturn(mappedUser);
            when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");
            when(roleService.findById(2L)).thenReturn(Optional.of(invitedRole));
            when(userInvitationService.findByRoleAndEmail(2L, "new@test.com"))
                    .thenReturn(new ArrayList<>(List.of(invitation)));
            when(licenseService.getLicensingState()).thenReturn(
                    LicensingState.builder().hasLicense(false).usersCount(0).build());
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_USERS)).thenReturn(true);
            when(userRepository.findByCompany_Id(1L)).thenReturn(List.of());
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(jwtTokenProvider.createToken(eq("new@test.com"), anyList()))
                    .thenReturn("signup-token");

            SignupSuccessResponse<User> response = userService.signup(signupRequest);

            assertTrue(response.isSuccess());
            assertEquals(company, response.getUser().getCompany());
        }

        @Test
        void invitedRole_exceedsSubscriptionLimit_throws() {
            Role invitedRole = Role.builder()
                    .id(2L).name("Technician").roleType(RoleType.ROLE_CLIENT)
                    .code(RoleCode.TECHNICIAN).paid(true).build();
            signupRequest.setRole(invitedRole);
            mappedUser.setRole(invitedRole);
            UserInvitation invitation = new UserInvitation("new@test.com", invitedRole);
            invitation.setId(1L);
            invitation.setCreatedBy(1L);
            company.getSubscription().setUsersCount(1);
            User otherUser = buildUser(2L, "other@test.com");
            otherUser.setEnabled(true);
            otherUser.setEnabledInSubscription(true);

            when(userMapper.toModel(signupRequest)).thenReturn(mappedUser);
            when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");
            when(roleService.findById(2L)).thenReturn(Optional.of(invitedRole));
            when(userInvitationService.findByRoleAndEmail(2L, "new@test.com"))
                    .thenReturn(new ArrayList<>(List.of(invitation)));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(licenseService.getLicensingState()).thenReturn(
                    LicensingState.builder().hasLicense(false).usersCount(0).build());
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_USERS)).thenReturn(true);
            when(userRepository.findByCompany_Id(1L)).thenReturn(List.of(user, otherUser));

            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.signup(signupRequest));
            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        void newCompanyNonLocalhost_enableInvitationViaEmail() {
            ReflectionTestUtils.setField(userService, "PUBLIC_API_URL", "https://app.test.com");
            ReflectionTestUtils.setField(userService, "cloudVersion", false);
            ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", true);
            when(userMapper.toModel(signupRequest)).thenReturn(mappedUser);
            when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");
            when(licenseService.hasEntitlement(LicenseEntitlement.MULTI_INSTANCE)).thenReturn(true);
            when(subscriptionPlanService.findByCode("BUSINESS"))
                    .thenReturn(Optional.of(subscriptionPlan));
            when(licenseService.getLicensingState()).thenReturn(
                    LicensingState.builder().hasLicense(false).usersCount(0).build());
            com.grash.model.Currency currency = new com.grash.model.Currency();
            currency.setCode("$");
            when(currencyService.findByCode("$")).thenReturn(Optional.of(currency));
            when(roleService.findDefaultRoleWithCode(RoleCode.ADMIN)).thenReturn(Optional.of(role));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(verificationTokenRepository.save(any(VerificationToken.class))).thenReturn(null);
            doNothing().when(mailService).sendMessageUsingThymeleafTemplate(
                    any(String[].class), anyString(), anyMap(), anyString(), any(), any());
            when(messageSource.getMessage(anyString(), any(), any())).thenReturn("Confirm Email");

            SignupSuccessResponse<User> response = userService.signup(signupRequest);

            assertTrue(response.isSuccess());
            assertEquals("Successful registration. Check your mailbox to activate your account",
                    response.getMessage());
            assertNull(response.getUser());
        }

        @Test
        void allowedOrganizationAdmins_allowsListedUser() {
            ReflectionTestUtils.setField(userService, "allowedOrganizationAdmins",
                    new String[]{"new@test.com"});
            when(userMapper.toModel(signupRequest)).thenReturn(mappedUser);
            when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");
            when(licenseService.hasEntitlement(LicenseEntitlement.MULTI_INSTANCE)).thenReturn(true);
            when(subscriptionPlanService.findByCode("BUSINESS"))
                    .thenReturn(Optional.of(subscriptionPlan));
            when(licenseService.getLicensingState()).thenReturn(
                    LicensingState.builder().hasLicense(false).usersCount(0).build());
            com.grash.model.Currency ccy = new com.grash.model.Currency();
            ccy.setCode("$");
            when(currencyService.findByCode("$")).thenReturn(Optional.of(ccy));
            when(roleService.findDefaultRoleWithCode(RoleCode.ADMIN)).thenReturn(Optional.of(role));
            when(jwtTokenProvider.createToken(eq("new@test.com"), anyList()))
                    .thenReturn("signup-token");

            SignupSuccessResponse<User> response = userService.signup(signupRequest);

            assertTrue(response.isSuccess());
        }

        @Test
        void invitedUser_enableInvitationViaEmail_withInvitations_fallsThrough() {
            ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", true);
            Role invitedRole = Role.builder()
                    .id(2L).name("Technician").roleType(RoleType.ROLE_CLIENT)
                    .code(RoleCode.TECHNICIAN).paid(true).build();
            signupRequest.setRole(invitedRole);
            mappedUser.setRole(invitedRole);
            UserInvitation invitation = new UserInvitation("new@test.com", invitedRole);
            invitation.setId(1L);
            invitation.setCreatedBy(1L);

            when(userMapper.toModel(signupRequest)).thenReturn(mappedUser);
            when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");
            when(roleService.findById(2L)).thenReturn(Optional.of(invitedRole));
            when(userInvitationService.findByRoleAndEmail(2L, "new@test.com"))
                    .thenReturn(new ArrayList<>(List.of(invitation)));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(licenseService.getLicensingState()).thenReturn(
                    LicensingState.builder().hasLicense(false).usersCount(0).build());
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_USERS)).thenReturn(true);
            when(userRepository.findByCompany_Id(1L)).thenReturn(List.of());
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(jwtTokenProvider.createToken(eq("new@test.com"), anyList()))
                    .thenReturn("signup-token");

            SignupSuccessResponse<User> response = userService.signup(signupRequest);

            assertTrue(response.isSuccess());
        }

        @Test
        void invitedUser_filterExcludesDisabledUsers() {
            Role invitedRole = Role.builder()
                    .id(2L).name("Technician").roleType(RoleType.ROLE_CLIENT)
                    .code(RoleCode.TECHNICIAN).paid(true).build();
            signupRequest.setRole(invitedRole);
            mappedUser.setRole(invitedRole);
            UserInvitation invitation = new UserInvitation("new@test.com", invitedRole);
            invitation.setId(1L);
            invitation.setCreatedBy(1L);
            company.getSubscription().setUsersCount(2);
            User disabledUser = buildUser(2L, "disabled@test.com");
            disabledUser.setEnabled(false);
            User notInSubscriptionUser = buildUser(3L, "notinsub@test.com");
            notInSubscriptionUser.setEnabledInSubscription(false);

            when(userMapper.toModel(signupRequest)).thenReturn(mappedUser);
            when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");
            when(roleService.findById(2L)).thenReturn(Optional.of(invitedRole));
            when(userInvitationService.findByRoleAndEmail(2L, "new@test.com"))
                    .thenReturn(new ArrayList<>(List.of(invitation)));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(licenseService.getLicensingState()).thenReturn(
                    LicensingState.builder().hasLicense(false).usersCount(0).build());
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_USERS)).thenReturn(true);
            when(userRepository.findByCompany_Id(1L))
                    .thenReturn(List.of(user, disabledUser, notInSubscriptionUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(jwtTokenProvider.createToken(eq("new@test.com"), anyList()))
                    .thenReturn("signup-token");

            SignupSuccessResponse<User> response = userService.signup(signupRequest);

            assertTrue(response.isSuccess());
        }

        @Test
        void newCompanyNonLocalhost_enableInvitationViaEmail_skipMailSending() {
            ReflectionTestUtils.setField(userService, "PUBLIC_API_URL", "https://app.test.com");
            ReflectionTestUtils.setField(userService, "cloudVersion", false);
            ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", true);
            signupRequest.setSkipMailSending(true);
            when(userMapper.toModel(signupRequest)).thenReturn(mappedUser);
            when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");
            when(licenseService.hasEntitlement(LicenseEntitlement.MULTI_INSTANCE)).thenReturn(true);
            when(subscriptionPlanService.findByCode("BUSINESS"))
                    .thenReturn(Optional.of(subscriptionPlan));
            when(licenseService.getLicensingState()).thenReturn(
                    LicensingState.builder().hasLicense(false).usersCount(0).build());
            com.grash.model.Currency ccy = new com.grash.model.Currency();
            ccy.setCode("$");
            when(currencyService.findByCode("$")).thenReturn(Optional.of(ccy));
            when(roleService.findDefaultRoleWithCode(RoleCode.ADMIN)).thenReturn(Optional.of(role));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(verificationTokenRepository.save(any(VerificationToken.class))).thenReturn(null);

            SignupSuccessResponse<User> response = userService.signup(signupRequest);

            assertTrue(response.isSuccess());
            assertEquals("Successful registration. Check your mailbox to activate your account",
                    response.getMessage());
            assertNull(response.getUser());
            verify(mailService, never()).sendMessageUsingThymeleafTemplate(
                    any(), anyString(), anyMap(), anyString(), any(), any());
        }

        @Test
        void newCompanyNonLocalhost_enableInvitationViaEmail_demoUser() {
            ReflectionTestUtils.setField(userService, "PUBLIC_API_URL", "https://app.test.com");
            ReflectionTestUtils.setField(userService, "cloudVersion", false);
            ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", true);
            signupRequest.setDemo(true);
            when(userMapper.toModel(signupRequest)).thenReturn(mappedUser);
            when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");
            when(licenseService.hasEntitlement(LicenseEntitlement.MULTI_INSTANCE)).thenReturn(true);
            when(subscriptionPlanService.findByCode("BUSINESS"))
                    .thenReturn(Optional.of(subscriptionPlan));
            when(licenseService.getLicensingState()).thenReturn(
                    LicensingState.builder().hasLicense(false).usersCount(0).build());
            com.grash.model.Currency ccy = new com.grash.model.Currency();
            ccy.setCode("$");
            when(currencyService.findByCode("$")).thenReturn(Optional.of(ccy));
            when(roleService.findDefaultRoleWithCode(RoleCode.ADMIN)).thenReturn(Optional.of(role));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(verificationTokenRepository.save(any(VerificationToken.class))).thenReturn(null);
            doNothing().when(mailService).sendMessageUsingThymeleafTemplate(
                    any(String[].class), anyString(), anyMap(), anyString(), any(), any());
            when(messageSource.getMessage(anyString(), any(), any())).thenReturn("Confirm Email");
            when(jwtTokenProvider.createToken(eq("new@test.com"), anyList()))
                    .thenReturn("signup-token");

            SignupSuccessResponse<User> response = userService.signup(signupRequest);

            assertTrue(response.isSuccess());
            assertNotNull(response.getUser());
        }

        @Test
        void newCompanyNonLocalhost_roleSetOnRequest_notNullBranch() throws jakarta.mail.MessagingException,
                java.io.IOException {
            ReflectionTestUtils.setField(userService, "PUBLIC_API_URL", "https://app.test.com");
            ReflectionTestUtils.setField(userService, "cloudVersion", false);
            signupRequest.setRole(role);

            when(userMapper.toModel(signupRequest)).thenReturn(mappedUser);
            when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");
            when(licenseService.hasEntitlement(LicenseEntitlement.MULTI_INSTANCE)).thenReturn(true);
            when(subscriptionPlanService.findByCode("BUSINESS"))
                    .thenReturn(Optional.of(subscriptionPlan));
            when(licenseService.getLicensingState()).thenReturn(
                    LicensingState.builder().hasLicense(false).usersCount(0).build());
            com.grash.model.Currency ccy = new com.grash.model.Currency();
            ccy.setCode("$");
            when(currencyService.findByCode("$")).thenReturn(Optional.of(ccy));
            when(roleService.findDefaultRoleWithCode(RoleCode.ADMIN)).thenReturn(Optional.of(role));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(cacheService).putUserInCache(any());
            doNothing().when(mailService).sendHtmlMessage(any(), anyString(), anyString(), any());

            SignupSuccessResponse<User> response = userService.signup(signupRequest);

            assertTrue(response.isSuccess());
            assertNull(response.getUser());
            assertEquals("Successful registration. Check your mailbox to activate your account",
                    response.getMessage());
        }
    }

    @Nested
    class CheckUsageBasedLimit {

        @Test
        void underLicenseLimit_noException() {
            when(licenseService.getLicensingState()).thenReturn(
                    LicensingState.builder().hasLicense(true).usersCount(10).build());
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_USERS)).thenReturn(false);
            when(userRepository.hasMorePaidUsersThan(anyInt())).thenReturn(false);

            assertDoesNotThrow(() -> userService.checkUsageBasedLimit(5));
        }

        @Test
        void atLicenseLimit_throwsRuntimeException() {
            when(licenseService.getLicensingState()).thenReturn(
                    LicensingState.builder().hasLicense(true).usersCount(10).build());
            when(userRepository.hasMorePaidUsersThan(5)).thenReturn(true);

            assertThrows(RuntimeException.class, () -> userService.checkUsageBasedLimit(5));
        }

        @Test
        void noLicense_usesFreeThreshold() {
            when(licenseService.getLicensingState()).thenReturn(
                    LicensingState.builder().hasLicense(false).usersCount(0).build());
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_USERS)).thenReturn(false);
            when(userRepository.hasMorePaidUsersThan(-1)).thenReturn(false);

            assertDoesNotThrow(() -> userService.checkUsageBasedLimit(6));
        }

        @Test
        void unlimitedUsersEntitlement_bypassesLimit() {
            when(licenseService.getLicensingState()).thenReturn(
                    LicensingState.builder().hasLicense(false).usersCount(0).build());
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_USERS)).thenReturn(true);

            assertDoesNotThrow(() -> userService.checkUsageBasedLimit(100));
            verify(userRepository, never()).hasMorePaidUsersThan(anyInt());
        }

        @Test
        void exactlyAtLicenseLimit_noException() {
            when(licenseService.getLicensingState()).thenReturn(
                    LicensingState.builder().hasLicense(true).usersCount(10).build());
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_USERS)).thenReturn(false);
            when(userRepository.hasMorePaidUsersThan(8)).thenReturn(false);

            assertDoesNotThrow(() -> userService.checkUsageBasedLimit(2));
        }

        @Test
        void exactlyAtFreeThreshold_noException() {
            when(licenseService.getLicensingState()).thenReturn(
                    LicensingState.builder().hasLicense(false).usersCount(0).build());
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_USERS)).thenReturn(false);
            when(userRepository.hasMorePaidUsersThan(4)).thenReturn(false);

            assertDoesNotThrow(() -> userService.checkUsageBasedLimit(1));
        }

        @Test
        void noLicense_overFreeThreshold_throws() {
            when(licenseService.getLicensingState()).thenReturn(
                    LicensingState.builder().hasLicense(false).usersCount(0).build());
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_USERS)).thenReturn(false);
            when(userRepository.hasMorePaidUsersThan(anyInt())).thenReturn(true);

            assertThrows(RuntimeException.class, () -> userService.checkUsageBasedLimit(1));
        }

        @Test
        void licensedSystemAlreadyOverLimit_throws() {
            when(licenseService.getLicensingState()).thenReturn(
                    LicensingState.builder().hasLicense(true).usersCount(5).build());
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_USERS)).thenReturn(false);
            when(userRepository.hasMorePaidUsersThan(5)).thenReturn(true);

            assertThrows(RuntimeException.class, () -> userService.checkUsageBasedLimit(0));
        }

        @Test
        void exceptionMessage_mentionsFreeThreshold() {
            when(licenseService.getLicensingState()).thenReturn(LicensingState.builder().hasLicense(false).usersCount(0).build());
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_USERS)).thenReturn(false);
            when(userRepository.hasMorePaidUsersThan(anyInt())).thenReturn(true);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.checkUsageBasedLimit(1));

            assertTrue(ex.getMessage().contains("5"));
        }
    }

    @Nested
    class FindByEmailWithRolesCached {

        @Test
        void nullEmail_returnsEmpty() {
            assertEquals(Optional.empty(), userService.findByEmailWithRolesCached(null));
        }

        @Test
        void emptyEmail_returnsEmpty() {
            assertEquals(Optional.empty(), userService.findByEmailWithRolesCached("   "));
        }

        @Test
        void cachedUser_returnsFromCache() {
            when(cacheService.getUserFromCache("john@test.com")).thenReturn(Optional.of(user));

            Optional<User> result = userService.findByEmailWithRolesCached("john@test.com");

            assertTrue(result.isPresent());
            assertEquals("john@test.com", result.get().getEmail());
            verify(userRepository, never()).findByEmailIgnoreCase(anyString());
        }

        @Test
        void uncachedUser_fetchesFromRepoAndCaches() {
            when(cacheService.getUserFromCache("john@test.com")).thenReturn(Optional.empty());
            when(userRepository.findByEmailIgnoreCase("john@test.com")).thenReturn(Optional.of(user));

            Optional<User> result = userService.findByEmailWithRolesCached("john@test.com");

            assertTrue(result.isPresent());
            assertEquals("john@test.com", result.get().getEmail());
            verify(cacheService).putUserInCache(user);
        }

        @Test
        void userNotFound_returnsEmpty() {
            when(cacheService.getUserFromCache("unknown@test.com")).thenReturn(Optional.empty());
            when(userRepository.findByEmailIgnoreCase("unknown@test.com")).thenReturn(Optional.empty());

            Optional<User> result = userService.findByEmailWithRolesCached("unknown@test.com");

            assertFalse(result.isPresent());
            verify(cacheService, never()).putUserInCache(any());
        }
    }

    @Nested
    class EnableUser {

        @Test
        void paidRoleWithinLimit_enablesAndSaves() {
            when(userRepository.findByCompany_Id(1L)).thenReturn(List.of(user));
            when(licenseService.getLicensingState()).thenReturn(
                    LicensingState.builder().hasLicense(false).usersCount(0).build());
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_USERS)).thenReturn(true);

            User result = userService.enableUser(user);

            assertTrue(result.isEnabled());
            assertTrue(result.isEnabledInSubscription());
            verify(userRepository).save(user);
            verify(cacheService).putUserInCache(user);
        }

        @Test
        void paidRoleExceedsSubscriptionLimit_throws() {
            role.setPaid(true);
            User otherUser = buildUser(2L, "other@test.com");
            otherUser.setEnabled(true);
            when(userRepository.findByCompany_Id(1L)).thenReturn(List.of(user, otherUser));
            company.getSubscription().setUsersCount(1);
            when(licenseService.getLicensingState()).thenReturn(
                    LicensingState.builder().hasLicense(false).usersCount(0).build());
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_USERS)).thenReturn(true);

            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.enableUser(user));
            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        void nonPaidRole_skipsLimitCheck() {
            role.setPaid(false);

            User result = userService.enableUser(user);

            assertTrue(result.isEnabled());
            assertTrue(result.isEnabledInSubscription());
            verify(userRepository).save(user);
            verify(cacheService).putUserInCache(user);
        }

        @Test
        void enableUserByEmail_delegatesAndEnables() {
            when(userRepository.findByEmailIgnoreCase("john@test.com")).thenReturn(Optional.of(user));
            when(userRepository.findByCompany_Id(1L)).thenReturn(List.of(user));
            when(licenseService.getLicensingState()).thenReturn(
                    LicensingState.builder().hasLicense(false).usersCount(0).build());
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_USERS)).thenReturn(true);

            userService.enableUser("john@test.com");

            assertTrue(user.isEnabled());
            verify(userRepository).save(user);
        }
    }

    @Nested
    class ThrowIfEmailNotificationsNotEnabled {

        @Test
        void mailsEnabled_doesNotThrow() {
            ReflectionTestUtils.setField(userService, "enableMails", true);
            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(userService,
                    "throwIfEmailNotificationsNotEnabled"));
        }

        @Test
        void mailsDisabled_throws() {
            ReflectionTestUtils.setField(userService, "enableMails", false);
            CustomException ex = assertThrows(CustomException.class,
                    () -> ReflectionTestUtils.invokeMethod(userService, "throwIfEmailNotificationsNotEnabled"));
            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }
    }

    @Nested
    class Invite {

        @Test
        void newValidEmail_createsInvitation() {
            role.setPaid(false);
            when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(false);
            ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", false);

            userService.invite("new@test.com", role, user, false);

            verify(userInvitationService).create(any(UserInvitation.class));
        }

        @Test
        void existingEmail_throws() {
            when(userRepository.existsByEmailIgnoreCase("existing@test.com")).thenReturn(true);

            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.invite("existing@test.com", role, user, false));
            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        void invalidEmail_throws() {
            when(userRepository.existsByEmailIgnoreCase("invalid")).thenReturn(false);

            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.invite("invalid", role, user, false));
            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        void paidRoleWithLimitExceeded_throws() {
            role.setPaid(true);
            when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(false);
            when(licenseService.getLicensingState()).thenReturn(
                    LicensingState.builder().hasLicense(false).usersCount(0).build());
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_USERS)).thenReturn(false);
            when(userRepository.hasMorePaidUsersThan(4)).thenReturn(true);

            assertThrows(RuntimeException.class,
                    () -> userService.invite("new@test.com", role, user, false));
        }

        @Test
        void invite_sendsEmail() {
            ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", true);
            ReflectionTestUtils.setField(userService, "enableMails", true);
            role.setPaid(false);
            when(userRepository.existsByEmailIgnoreCase("invited@test.com")).thenReturn(false);
            when(mailServiceFactory.getMailService()).thenReturn(mailService);
            BrandConfig bc = new BrandConfig();
            bc.setName("TestBrand");
            when(brandingService.getBrandConfig()).thenReturn(bc);
            when(messageSource.getMessage(anyString(), any(), any())).thenReturn("Invitation subject");

            userService.invite("invited@test.com", role, user, false);

            verify(userInvitationService).create(any(UserInvitation.class));
            verify(mailService).sendMessageUsingThymeleafTemplate(
                    eq(new String[]{"invited@test.com"}), anyString(), anyMap(), anyString(), any(), eq(null));
        }
    }

    @Nested
    class Update {

        @Test
        void updatesExistingUser() {
            UserPatchDTO patch = new UserPatchDTO();
            patch.setFirstName("Jane");
            when(userRepository.existsById(1L)).thenReturn(true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userMapper.updateUser(any(User.class), eq(patch))).thenReturn(user);

            User result = userService.update(1L, patch);

            assertNotNull(result);
            verify(em).refresh(user);
            verify(cacheService).putUserInCache(user);
        }

        @Test
        void updatesPassword_whenProvided() {
            UserPatchDTO patch = new UserPatchDTO();
            patch.setNewPassword("newpassword123");
            when(userRepository.existsById(1L)).thenReturn(true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("newpassword123")).thenReturn("encoded-new");
            when(userRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userMapper.updateUser(any(User.class), eq(patch))).thenReturn(user);

            userService.update(1L, patch);

            verify(passwordEncoder).encode("newpassword123");
            assertEquals("encoded-new", user.getPassword());
            assertNotNull(user.getSessionInvalidatedAt());
            verify(refreshTokenService).revokeAllForUser(user);
        }

        @Test
        void nonExistentUser_throws() {
            when(userRepository.existsById(999L)).thenReturn(false);

            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.update(999L, new UserPatchDTO()));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void invitationViaEmailEnabled_throwsOnPasswordChange() {
            ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", true);
            UserPatchDTO patch = new UserPatchDTO();
            patch.setNewPassword("newpassword123");
            when(userRepository.existsById(1L)).thenReturn(true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.update(1L, patch));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }
    }

    @Nested
    class Delete {

        @Test
        void deletesByUsername() {
            userService.delete("johndoe");
            verify(userRepository).deleteByUsername("johndoe");
        }
    }

    @Nested
    class Whoami {

        @Test
        void withToken_returnsUserFromCache() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(jwtTokenProvider.resolveToken(request)).thenReturn("valid-token");
            when(jwtTokenProvider.getUsername("valid-token")).thenReturn("john@test.com");
            when(cacheService.getUserFromCache("john@test.com")).thenReturn(Optional.of(user));

            User result = userService.whoami(request, true);

            assertEquals("john@test.com", result.getEmail());
        }

        @Test
        void withoutToken_returnsUserFromSecurityContext() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(jwtTokenProvider.resolveToken(request)).thenReturn(null);
            CustomUserDetail principal = CustomUserDetail.builder().user(user).build();
            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn(principal);
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(auth);
            try (MockedStatic<org.springframework.security.core.context.SecurityContextHolder> holder =
                         mockStatic(org.springframework.security.core.context.SecurityContextHolder.class)) {
                holder.when(org.springframework.security.core.context.SecurityContextHolder::getContext)
                        .thenReturn(securityContext);

                User result = userService.whoami(request, true);

                assertEquals("john@test.com", result.getEmail());
            }
        }

        @Test
        void withoutTokenNoAuth_throws() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(jwtTokenProvider.resolveToken(request)).thenReturn(null);
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(null);
            try (MockedStatic<org.springframework.security.core.context.SecurityContextHolder> holder =
                         mockStatic(org.springframework.security.core.context.SecurityContextHolder.class)) {
                holder.when(org.springframework.security.core.context.SecurityContextHolder::getContext)
                        .thenReturn(securityContext);

                CustomException ex = assertThrows(CustomException.class,
                        () -> userService.whoami(request, true));
                assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
            }
        }

        @Test
        void withoutTokenAuthNotCustomUserDetail_throws() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(jwtTokenProvider.resolveToken(request)).thenReturn(null);
            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn("not a user detail");
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(auth);
            try (MockedStatic<org.springframework.security.core.context.SecurityContextHolder> holder =
                         mockStatic(org.springframework.security.core.context.SecurityContextHolder.class)) {
                holder.when(org.springframework.security.core.context.SecurityContextHolder::getContext)
                        .thenReturn(securityContext);

                CustomException ex = assertThrows(CustomException.class,
                        () -> userService.whoami(request, true));
                assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
            }
        }

        @Test
        void withToken_uncached() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(jwtTokenProvider.resolveToken(request)).thenReturn("valid-token");
            when(jwtTokenProvider.getUsername("valid-token")).thenReturn("john@test.com");
            when(userRepository.findByEmailIgnoreCase("john@test.com")).thenReturn(Optional.of(user));

            User result = userService.whoami(request, false);

            assertEquals("john@test.com", result.getEmail());
            verify(userRepository).findByEmailIgnoreCase("john@test.com");
        }
    }

    @Nested
    class ResetPasswordRequest {

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(userService, "enableMails", true);
            lenient().when(mailServiceFactory.getMailService()).thenReturn(mailService);
            BrandConfig bc = new BrandConfig();
            bc.setName("TestBrand");
            lenient().when(brandingService.getBrandConfig()).thenReturn(bc);
            lenient().when(messageSource.getMessage(anyString(), any(), any())).thenReturn("Password Reset Subject");
        }

        @Test
        void sendsResetEmail() {
            when(userRepository.findByEmailIgnoreCase("john@test.com")).thenReturn(Optional.of(user));
            doNothing().when(mailService).sendMessageUsingThymeleafTemplate(
                    any(String[].class), anyString(), anyMap(), anyString(), any(), any());

            SuccessResponse response = userService.resetPasswordRequest("john@test.com");

            assertTrue(response.isSuccess());
            verify(verificationTokenRepository).save(any(VerificationToken.class));
            verify(mailService).sendMessageUsingThymeleafTemplate(
                    eq(new String[]{"john@test.com"}), anyString(), anyMap(), eq("reset-password.html"), any(), any());
        }

        @Test
        void mailsDisabled_throws() {
            ReflectionTestUtils.setField(userService, "enableMails", false);

            assertThrows(CustomException.class,
                    () -> userService.resetPasswordRequest("john@test.com"));
            verifyNoInteractions(mailService);
        }
    }

    @Nested
    class DeleteAccountRequest {

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(userService, "enableMails", true);
            lenient().when(mailServiceFactory.getMailService()).thenReturn(mailService);
            BrandConfig bc = new BrandConfig();
            bc.setName("TestBrand");
            lenient().when(brandingService.getBrandConfig()).thenReturn(bc);
            lenient().when(messageSource.getMessage(anyString(), any(), any()))
                    .thenReturn("Account Deletion Subject");
        }

        @SuppressWarnings("unchecked")
        @Test
        void ownsCompany_sendsEmailWithCompanyFlag() {
            user.setOwnsCompany(true);
            company.setName("Acme Corp");

            SuccessResponse response = userService.deleteAccountRequest(user);

            assertTrue(response.isSuccess());
            verify(verificationTokenRepository).save(any(VerificationToken.class));

            ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
            ArgumentCaptor<String> templateCaptor = ArgumentCaptor.forClass(String.class);
            verify(mailService).sendMessageUsingThymeleafTemplate(
                    eq(new String[]{"john@test.com"}), anyString(), variablesCaptor.capture(),
                    templateCaptor.capture(), any(), any());

            assertEquals("delete-account.html", templateCaptor.getValue());
            assertEquals(true, variablesCaptor.getValue().get("ownsCompany"));
            assertEquals("Acme Corp", variablesCaptor.getValue().get("companyName"));
            assertTrue(((String) variablesCaptor.getValue().get("deleteConfirmLink"))
                    .contains("/auth/delete-account-confirm?token="));
        }

        @SuppressWarnings("unchecked")
        @Test
        void notOwnsCompany_sendsEmailWithoutCompanyFlag() {
            user.setOwnsCompany(false);

            userService.deleteAccountRequest(user);

            ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
            verify(mailService).sendMessageUsingThymeleafTemplate(
                    eq(new String[]{"john@test.com"}), anyString(), variablesCaptor.capture(),
                    eq("delete-account.html"), any(), any());

            assertEquals(false, variablesCaptor.getValue().get("ownsCompany"));
        }

        @Test
        void mailsDisabled_throws() {
            ReflectionTestUtils.setField(userService, "enableMails", false);

            assertThrows(CustomException.class, () -> userService.deleteAccountRequest(user));
            verifyNoInteractions(mailService);
        }
    }

    @Nested
    class DeleteAccount {

        @Test
        void ownsCompany_deletesCompany() {
            user.setOwnsCompany(true);
            company.setId(1L);

            userService.deleteAccount(user);

            verify(companyService).delete(1L);
            verify(userRepository, never()).delete(user);
            verify(cacheService).evictUserFromCache(user.getEmail());
            verify(refreshTokenService).revokeAllForUser(user);
        }

        @Test
        void notOwnsCompany_deletesUser() {
            user.setOwnsCompany(false);

            userService.deleteAccount(user);

            verify(userRepository).delete(user);
            verify(companyService, never()).delete(anyLong());
            verify(apiKeyService).revokeAllByUser(user);
            verify(cacheService).evictUserFromCache(user.getEmail());
            verify(refreshTokenService).revokeAllForUser(user);
        }
    }

    @Nested
    class SendRegistrationMailToSuperAdmins {

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(userService, "recipients", new String[]{"admin@test.com"});
            lenient().when(mailServiceFactory.getMailService()).thenReturn(mailService);
            BrandConfig bc = new BrandConfig();
            bc.setShortName("TestBrand");
            lenient().when(brandingService.getBrandConfig()).thenReturn(bc);
        }

        @Test
        void superAdminEmail_skipsSending() {
            User superAdmin = buildUser(2L, "superadmin@test.com");
            superAdmin.setCompany(company);
            UserSignupRequest req = new UserSignupRequest();

            userService.sendRegistrationMailToSuperAdmins(superAdmin, req);

            verifyNoInteractions(mailService);
        }

        @Test
        void demoCompany_skipsSending() {
            company.setDemo(true);
            User demoUser = buildUser(2L, "demo@test.com");
            demoUser.setCompany(company);
            UserSignupRequest req = new UserSignupRequest();

            userService.sendRegistrationMailToSuperAdmins(demoUser, req);

            verifyNoInteractions(mailService);
        }

        @Test
        void noRecipients_skipsSending() {
            ReflectionTestUtils.setField(userService, "recipients", null);
            UserSignupRequest req = new UserSignupRequest();

            userService.sendRegistrationMailToSuperAdmins(user, req);

            verifyNoInteractions(mailService);
        }

        @Test
        void emptyRecipientsArray_skipsSending() {
            ReflectionTestUtils.setField(userService, "recipients", new String[]{});
            UserSignupRequest req = new UserSignupRequest();

            userService.sendRegistrationMailToSuperAdmins(user, req);

            verifyNoInteractions(mailService);
        }

        @Test
        void mailServiceException_caughtGracefully() throws Exception {
            doThrow(new jakarta.mail.MessagingException("fail"))
                    .when(mailService).sendHtmlMessage(any(), anyString(), anyString(), any());
            UserSignupRequest req = UserSignupRequest.builder()
                    .employeesCount(10)
                    .build();

            assertDoesNotThrow(() -> userService.sendRegistrationMailToSuperAdmins(user, req));
        }

        @Test
        void sendsEmailToRecipients() throws Exception {
            UserSignupRequest req = UserSignupRequest.builder()
                    .employeesCount(10)
                    .build();

            userService.sendRegistrationMailToSuperAdmins(user, req);

            verify(mailService).sendHtmlMessage(
                    eq(new String[]{"admin@test.com"}), anyString(), anyString(), eq(null));
        }
    }

    @Nested
    class BuildRegistrationEmailSubject {

        private BrandConfig brandConfig;

        @BeforeEach
        void setUp() {
            brandConfig = mock(BrandConfig.class);
            when(brandingService.getBrandConfig()).thenReturn(brandConfig);
        }

        @Test
        void noSubscriptionPlanId_usesDefaultSubject() {
            when(brandConfig.getShortName()).thenReturn("TestBrand");
            UserSignupRequest req = UserSignupRequest.builder().build();
            String subject = ReflectionTestUtils.invokeMethod(userService, "buildRegistrationEmailSubject", req,
                    brandingService);
            assertEquals("New TestBrand registration", subject);
        }

        @Test
        void withSubscriptionPlanId_includesPlan() {
            when(brandConfig.getShortName()).thenReturn("TestBrand");
            UserSignupRequest req = UserSignupRequest.builder()
                    .subscriptionPlanId("plan_pro")
                    .build();
            String subject = ReflectionTestUtils.invokeMethod(userService, "buildRegistrationEmailSubject", req,
                    brandingService);
            assertEquals("TestBrand plan plan_pro used", subject);
        }
    }

    @Nested
    class BuildRegistrationEmailBody {

        @Test
        void containsExpectedFields() {
            UserSignupRequest req = UserSignupRequest.builder()
                    .employeesCount(10)
                    .build();
            String body = ReflectionTestUtils.invokeMethod(userService, "buildRegistrationEmailBody", user, req);

            assertTrue(body.contains("John Doe"));
            assertTrue(body.contains("TestCo"));
            assertTrue(body.contains("10 employees"));
            assertTrue(body.contains("john@test.com"));
            assertTrue(body.contains("123-456-7890"));
        }

        @Test
        void invitedUser_includesRegistrationType() {
            User invitedUser = buildUser(2L, "invited@test.com");
            invitedUser.setOwnsCompany(false);
            UserSignupRequest req = UserSignupRequest.builder().build();
            String body = ReflectionTestUtils.invokeMethod(userService, "buildRegistrationEmailBody", invitedUser, req);

            assertTrue(body.contains("After invitation"));
        }

        @Test
        void companyOwner_omitsRegistrationType() {
            user.setOwnsCompany(true);
            UserSignupRequest req = UserSignupRequest.builder().build();
            String body = ReflectionTestUtils.invokeMethod(userService, "buildRegistrationEmailBody", user, req);

            assertFalse(body.contains("After invitation"));
        }
    }

    @Nested
    class AppendUtmParameters {

        private UtmParams utmParams;
        private UserSignupRequest req;

        @BeforeEach
        void setUp() {
            utmParams = new UtmParams();
            req = UserSignupRequest.builder().utmParams(utmParams).build();
        }

        @Test
        void nonCloudVersion_skipsUtm() {
            ReflectionTestUtils.setField(userService, "cloudVersion", false);
            StringBuilder body = new StringBuilder("body");

            ReflectionTestUtils.invokeMethod(userService, "appendUtmParameters", body, req);

            assertEquals("body", body.toString());
        }

        @Test
        void noUtmParams_skipsUtm() {
            ReflectionTestUtils.setField(userService, "cloudVersion", true);
            req.setUtmParams(null);
            StringBuilder body = new StringBuilder("body");

            ReflectionTestUtils.invokeMethod(userService, "appendUtmParameters", body, req);

            assertEquals("body", body.toString());
        }

        @Test
        void noParams_skipsUtm() {
            ReflectionTestUtils.setField(userService, "cloudVersion", true);
            StringBuilder body = new StringBuilder("body");

            ReflectionTestUtils.invokeMethod(userService, "appendUtmParameters", body, req);

            assertEquals("body", body.toString());
        }

        @Test
        void includesUtmSource() {
            ReflectionTestUtils.setField(userService, "cloudVersion", true);
            utmParams.setUtm_source("google");
            StringBuilder body = new StringBuilder();

            ReflectionTestUtils.invokeMethod(userService, "appendUtmParameters", body, req);

            assertTrue(body.toString().contains("UTM Source: google"));
        }

        @Test
        void includesUtmFieldsWithoutSource() {
            ReflectionTestUtils.setField(userService, "cloudVersion", true);
            utmParams.setReferrer("ref.com");
            utmParams.setUtm_medium("cpc");
            utmParams.setUtm_campaign("spring");
            StringBuilder body = new StringBuilder();

            ReflectionTestUtils.invokeMethod(userService, "appendUtmParameters", body, req);

            String result = body.toString();
            assertTrue(result.contains("Referrer: ref.com"));
            assertFalse(result.contains("UTM Source:"));
            assertTrue(result.contains("UTM Medium: cpc"));
            assertTrue(result.contains("UTM Campaign: spring"));
        }

        @Test
        void includesAllUtmFields() {
            ReflectionTestUtils.setField(userService, "cloudVersion", true);
            utmParams.setReferrer("ref.com");
            utmParams.setUtm_source("google");
            utmParams.setUtm_medium("cpc");
            utmParams.setUtm_campaign("spring");
            utmParams.setUtm_term("term");
            utmParams.setUtm_content("content");
            utmParams.setGclid("gclid123");
            utmParams.setFbclid("fbclid456");
            StringBuilder body = new StringBuilder();

            ReflectionTestUtils.invokeMethod(userService, "appendUtmParameters", body, req);

            String result = body.toString();
            assertTrue(result.contains("Referrer: ref.com"));
            assertTrue(result.contains("UTM Source: google"));
            assertTrue(result.contains("UTM Medium: cpc"));
            assertTrue(result.contains("UTM Campaign: spring"));
            assertTrue(result.contains("UTM Term: term"));
            assertTrue(result.contains("UTM Content: content"));
            assertTrue(result.contains("Google Click ID: gclid123"));
            assertTrue(result.contains("Facebook Click ID: fbclid456"));
        }
    }

    @Nested
    class InviteUsers {

        private UserInvitationDTO invitationDTO;
        private Role invitedRole;

        @BeforeEach
        void init() {
            invitedRole = Role.builder()
                    .id(2L)
                    .name("Technician")
                    .roleType(RoleType.ROLE_CLIENT)
                    .paid(true)
                    .build();

            invitationDTO = new UserInvitationDTO();
            invitationDTO.setRole(invitedRole);
            invitationDTO.setEmails(new ArrayList<>(List.of("invited@test.com")));
            invitationDTO.setDisableSendingEmail(true);

            Role adminRole = Role.builder()
                    .id(1L)
                    .name("Administrator")
                    .roleType(RoleType.ROLE_CLIENT)
                    .code(RoleCode.ADMIN)
                    .createPermissions(new HashSet<>(Set.of(PermissionEntity.PEOPLE_AND_TEAMS)))
                    .build();
            user.setRole(adminRole);
        }

        @Test
        void accessDenied_noCreatePermission_throws403() {
            Role restrictedRole = Role.builder()
                    .id(3L).name("Restricted").roleType(RoleType.ROLE_CLIENT)
                    .createPermissions(new HashSet<>())
                    .build();
            user.setRole(restrictedRole);

            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.inviteUsers(invitationDTO, user));
            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void roleNotFound_throws404() {
            when(roleService.findById(2L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.inviteUsers(invitationDTO, user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void roleNotInCompany_throws404() {
            Company otherCompany = new Company();
            otherCompany.setId(99L);
            CompanySettings otherSettings = new CompanySettings();
            otherSettings.setId(99L);
            otherSettings.setCompany(otherCompany);
            company.getCompanySettings().setId(1L);
            invitedRole.setCompanySettings(otherSettings);

            when(roleService.findById(2L)).thenReturn(Optional.of(invitedRole));

            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.inviteUsers(invitationDTO, user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void subscriptionLimitExceeded_throws406() {
            company.getSubscription().setUsersCount(1);
            User paidUser = buildUser(3L, "paid@test.com");
            when(roleService.findById(2L)).thenReturn(Optional.of(invitedRole));
            when(userRepository.findByCompany_Id(1L)).thenReturn(List.of(user, paidUser));

            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.inviteUsers(invitationDTO, user));
            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        void nonPaidRole_bypassesSubscriptionLimit_success() {
            invitedRole.setPaid(false);
            when(roleService.findById(2L)).thenReturn(Optional.of(invitedRole));
            when(userRepository.existsByEmailIgnoreCase("invited@test.com")).thenReturn(false);

            SuccessResponse response = userService.inviteUsers(invitationDTO, user);

            assertTrue(response.isSuccess());
            verify(userInvitationService).create(any(UserInvitation.class));
        }

        @Test
        void success_invitesUsersAndReturnsResponse() {
            company.getSubscription().setUsersCount(10);
            when(roleService.findById(2L)).thenReturn(Optional.of(invitedRole));
            when(userRepository.findByCompany_Id(1L)).thenReturn(List.of(user));
            when(userRepository.existsByEmailIgnoreCase("invited@test.com")).thenReturn(false);
            when(licenseService.getLicensingState()).thenReturn(
                    LicensingState.builder().hasLicense(false).usersCount(0).build());
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_USERS)).thenReturn(true);

            SuccessResponse response = userService.inviteUsers(invitationDTO, user);

            assertTrue(response.isSuccess());
            assertEquals("Users have been invited", response.getMessage());
            verify(userInvitationService).create(any(UserInvitation.class));
        }

        @Test
        void success_firesIntercomEventOnFirstInvite() {
            company.getSubscription().setUsersCount(10);
            when(roleService.findById(2L)).thenReturn(Optional.of(invitedRole));
            when(userRepository.findByCompany_Id(1L)).thenReturn(List.of(user));
            when(userRepository.existsByEmailIgnoreCase("invited@test.com")).thenReturn(false);
            when(licenseService.getLicensingState()).thenReturn(
                    LicensingState.builder().hasLicense(false).usersCount(0).build());
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_USERS)).thenReturn(true);

            userService.inviteUsers(invitationDTO, user);

            assertTrue(company.isInvitedUsers());
            verify(companyService).update(company);
            verify(intercomService).createCompanyActivationEvent(
                    eq("first-users-invited"), eq(1L), eq("john@test.com"), anyMap());
        }

        @Test
        void success_skipsIntercomWhenAlreadyInvited() {
            company.setInvitedUsers(true);
            company.getSubscription().setUsersCount(10);
            when(roleService.findById(2L)).thenReturn(Optional.of(invitedRole));
            when(userRepository.findByCompany_Id(1L)).thenReturn(List.of(user));
            when(userRepository.existsByEmailIgnoreCase("invited@test.com")).thenReturn(false);
            when(licenseService.getLicensingState()).thenReturn(
                    LicensingState.builder().hasLicense(false).usersCount(0).build());
            when(licenseService.hasEntitlement(LicenseEntitlement.UNLIMITED_USERS)).thenReturn(true);

            userService.inviteUsers(invitationDTO, user);

            verify(intercomService, never()).createCompanyActivationEvent(any(), any(), any(), any());
        }
    }

    @Nested
    class PatchUserRole {

        private User targetUser;
        private Role newRole;

        @BeforeEach
        void init() {
            newRole = Role.builder()
                    .id(2L)
                    .name("Technician")
                    .paid(true)
                    .build();
            targetUser = buildUser(2L, "target@test.com");
            targetUser.setCompany(company);

            Role adminRole = Role.builder()
                    .id(1L)
                    .name("Administrator")
                    .code(RoleCode.ADMIN)
                    .editOtherPermissions(new HashSet<>(Set.of(PermissionEntity.PEOPLE_AND_TEAMS)))
                    .build();
            user.setRole(adminRole);
        }

        @Test
        void userNotFound_throws404() {
            when(userRepository.findByIdAndCompany_Id(2L, 1L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.patchUserRole(2L, 2L, user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void roleNotFound_throws404() {
            when(userRepository.findByIdAndCompany_Id(2L, 1L)).thenReturn(Optional.of(targetUser));
            when(roleService.findById(2L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.patchUserRole(2L, 2L, user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void noEditPermission_throws406() {
            Role restrictedRole = Role.builder()
                    .id(3L).name("Restricted").editOtherPermissions(new HashSet<>()).build();
            user.setRole(restrictedRole);

            when(userRepository.findByIdAndCompany_Id(2L, 1L)).thenReturn(Optional.of(targetUser));
            when(roleService.findById(2L)).thenReturn(Optional.of(newRole));

            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.patchUserRole(2L, 2L, user));
            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        void subscriptionLimitExceeded_throws406() {
            company.getSubscription().setUsersCount(1);
            User anotherPaidUser = buildUser(3L, "extra@test.com");

            when(userRepository.findByIdAndCompany_Id(2L, 1L)).thenReturn(Optional.of(targetUser));
            when(roleService.findById(2L)).thenReturn(Optional.of(newRole));
            when(userRepository.findByCompany_Id(1L)).thenReturn(List.of(user, targetUser, anotherPaidUser));

            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.patchUserRole(2L, 2L, user));
            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        void success_updatesRoleAndSaves() {
            company.getSubscription().setUsersCount(10);
            when(userRepository.findByIdAndCompany_Id(2L, 1L)).thenReturn(Optional.of(targetUser));
            when(roleService.findById(2L)).thenReturn(Optional.of(newRole));
            when(userRepository.findByCompany_Id(1L)).thenReturn(List.of(user));
            when(userRepository.save(targetUser)).thenReturn(targetUser);

            User result = userService.patchUserRole(2L, 2L, user);

            assertEquals(newRole, result.getRole());
            verify(userRepository).save(targetUser);
        }
    }

    @Nested
    class DisableUser {

        private User targetUser;

        @BeforeEach
        void init() {
            targetUser = buildUser(2L, "target@test.com");
            targetUser.setCompany(company);

            Role adminRole = Role.builder()
                    .id(1L)
                    .name("Administrator")
                    .code(RoleCode.ADMIN)
                    .editOtherPermissions(new HashSet<>(Set.of(PermissionEntity.PEOPLE_AND_TEAMS)))
                    .build();
            user.setRole(adminRole);
        }

        @Test
        void userNotFound_throws404() {
            when(userRepository.findByIdAndCompany_Id(2L, 1L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.disableUser(2L, user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void noEditPermission_throws406() {
            Role restrictedRole = Role.builder()
                    .id(3L).name("Restricted").editOtherPermissions(new HashSet<>()).build();
            user.setRole(restrictedRole);

            when(userRepository.findByIdAndCompany_Id(2L, 1L)).thenReturn(Optional.of(targetUser));

            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.disableUser(2L, user));
            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        void success_disablesUser() {
            when(userRepository.findByIdAndCompany_Id(2L, 1L)).thenReturn(Optional.of(targetUser));
            when(userRepository.save(targetUser)).thenReturn(targetUser);

            User result = userService.disableUser(2L, user);

            assertFalse(result.isEnabled());
            assertFalse(result.isEnabledInSubscription());
            assertNotNull(result.getSessionInvalidatedAt());
            verify(userRepository).save(targetUser);
            verify(cacheService).evictUserFromCache("target@test.com");
            verify(refreshTokenService).revokeAllForUser(targetUser);
        }
    }

    @Nested
    class SoftDeleteUser {

        private User targetUser;

        @BeforeEach
        void init() {
            targetUser = buildUser(2L, "target@test.com");
            targetUser.setCompany(company);

            Role adminRole = Role.builder()
                    .id(1L)
                    .name("Administrator")
                    .code(RoleCode.ADMIN)
                    .editOtherPermissions(new HashSet<>(Set.of(PermissionEntity.PEOPLE_AND_TEAMS)))
                    .build();
            user.setRole(adminRole);
        }

        @Test
        void userNotFound_throws404() {
            when(userRepository.findByIdAndCompany_Id(2L, 1L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.softDeleteUser(2L, user));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }

        @Test
        void noPermission_throws406() {
            Role restrictedRole = Role.builder()
                    .id(3L).name("Restricted").editOtherPermissions(new HashSet<>()).build();
            user.setRole(restrictedRole);

            when(userRepository.findByIdAndCompany_Id(2L, 1L)).thenReturn(Optional.of(targetUser));

            CustomException ex = assertThrows(CustomException.class,
                    () -> userService.softDeleteUser(2L, user));
            assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getHttpStatus());
        }

        @Test
        void success_selfSoftDelete() {
            user.setId(2L);
            when(userRepository.findByIdAndCompany_Id(2L, 1L)).thenReturn(Optional.of(targetUser));
            when(userRepository.save(targetUser)).thenReturn(targetUser);

            User result = userService.softDeleteUser(2L, user);

            assertFalse(result.isEnabled());
            assertFalse(result.isEnabledInSubscription());
            assertTrue(result.getEmail().contains("_2"));
            assertNotNull(result.getSessionInvalidatedAt());
            verify(userRepository).save(targetUser);
            verify(cacheService).evictUserFromCache(result.getEmail());
            verify(refreshTokenService).revokeAllForUser(targetUser);
        }

        @Test
        void success_adminWithSettingsPermission() {
            when(userRepository.findByIdAndCompany_Id(2L, 1L)).thenReturn(Optional.of(targetUser));
            when(userRepository.save(targetUser)).thenReturn(targetUser);

            User result = userService.softDeleteUser(2L, user);

            assertFalse(result.isEnabled());
            assertFalse(result.isEnabledInSubscription());
            assertTrue(result.getEmail().contains("_2"));
            assertNotNull(result.getSessionInvalidatedAt());
            verify(userRepository).save(targetUser);
            verify(cacheService).evictUserFromCache(result.getEmail());
            verify(refreshTokenService).revokeAllForUser(targetUser);
        }
    }

    @Nested
    class InvalidateSessions {

        @Test
        void setsInvalidationTimestamp_savesEvictsCacheAndRevokesRefreshTokens() {
            User targetUser = buildUser(2L, "target@test.com");
            assertNull(targetUser.getSessionInvalidatedAt());
            when(userRepository.save(targetUser)).thenReturn(targetUser);

            User result = userService.invalidateSessions(targetUser);

            assertNotNull(result.getSessionInvalidatedAt());
            verify(userRepository).save(targetUser);
            verify(cacheService).evictUserFromCache("target@test.com");
            verify(refreshTokenService).revokeAllForUser(targetUser);
        }
    }
}
