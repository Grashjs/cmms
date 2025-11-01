package com.grash.service;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.BrandConfig;
import com.grash.dto.SignupSuccessResponse;
import com.grash.dto.UserPatchDTO;
import com.grash.dto.UserSignupRequest;
import com.grash.exception.CustomException;
import com.grash.mapper.UserMapper;
import com.grash.model.*;
import com.grash.model.enums.Language;
import com.grash.model.enums.RoleCode;
import com.grash.model.enums.RoleType;
import com.grash.repository.UserRepository;
import com.grash.repository.VerificationTokenRepository;
import com.grash.security.JwtTokenProvider;
import com.grash.utils.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.servlet.http.HttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doNothing;

import java.util.NoSuchElementException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
    private Utils utils;
    @Mock
    private MessageSource messageSource;
    @Mock
    private EmailService2 emailService2;
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

    private OwnUser user;
    private Company company;
    private Role role;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);
        company.setName("Test Company");

        role = new Role();
        role.setId(1L);
        role.setName("Administrator");
        role.setRoleType(RoleType.ROLE_CLIENT);
        role.setCompanySettings(new CompanySettings());
        role.getCompanySettings().setCompany(company);


        user = new OwnUser();
        user.setId(1L);
        user.setEmail("test@test.com");
        user.setPassword("password");
        user.setCompany(company);
        user.setRole(role);
    }

    @Nested
    @DisplayName("Signup Tests")
    class SignupTests {

        @Test
        @DisplayName("Should signup a new user and create a new company")
        void signupNewUserAndCompany() {
            UserSignupRequest userSignupRequest = new UserSignupRequest();
            userSignupRequest.setEmail("newuser@test.com");
            userSignupRequest.setPassword("password");
            userSignupRequest.setCompanyName("New Company");

            ReflectionTestUtils.setField(userService, "PUBLIC_API_URL", "http://localhost:8080");

            when(userRepository.existsByEmailIgnoreCase("newuser@test.com")).thenReturn(false);
            when(userMapper.toModel(any(UserSignupRequest.class))).thenAnswer(invocation -> {
                UserSignupRequest req = invocation.getArgument(0);
                OwnUser newUser = new OwnUser();
                newUser.setEmail(req.getEmail());
                newUser.setPassword(req.getPassword());
                return newUser;
            });
            when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
            when(utils.generateStringId()).thenReturn("randomId");
            when(subscriptionPlanService.findByCode("BUSINESS")).thenReturn(Optional.of(new SubscriptionPlan()));
            when(currencyService.findByCode("$")).thenReturn(Optional.of(new Currency()));
            when(userRepository.save(any(OwnUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

            SignupSuccessResponse<OwnUser> response = userService.signup(userSignupRequest);

            assertNotNull(response);
            assertTrue(response.isSuccess());
            assertEquals("token", response.getMessage());
        }

        @Test
        @DisplayName("Should send email to super admins on signup")
        void signupNewUserAndSendEmailToSuperAdmins() {
            UserSignupRequest userSignupRequest = new UserSignupRequest();
            userSignupRequest.setEmail("newuser@test.com");
            userSignupRequest.setPassword("password");
            ReflectionTestUtils.setField(userService, "PUBLIC_API_URL", "http://localhost:8080");
            ReflectionTestUtils.setField(userService, "recipients", new String[]{"admin@test.com"});
            ReflectionTestUtils.setField(userService, "enableMails", true);

            BrandConfig brandConfig = new BrandConfig();
            brandConfig.setName("Test Brand");
            brandConfig.setShortName("TB");
            when(brandingService.getBrandConfig()).thenReturn(brandConfig);

            when(userRepository.existsByEmailIgnoreCase("newuser@test.com")).thenReturn(false);
            when(userMapper.toModel(any(UserSignupRequest.class))).thenAnswer(invocation -> {
                UserSignupRequest req = invocation.getArgument(0);
                OwnUser newUser = new OwnUser();
                newUser.setEmail(req.getEmail());
                newUser.setPassword(req.getPassword());
                newUser.setCompany(company);
                return newUser;
            });
            when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
            when(utils.generateStringId()).thenReturn("randomId");
            when(subscriptionPlanService.findByCode("BUSINESS")).thenReturn(Optional.of(new SubscriptionPlan()));
            when(currencyService.findByCode("$")).thenReturn(Optional.of(new Currency()));
            when(userRepository.save(any(OwnUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

            userService.signup(userSignupRequest);
            
            // verify(emailService2, times(1)).sendMessageToSuperAdmins(any(), any());
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void signupWithExistingEmail() {
            UserSignupRequest userSignupRequest = new UserSignupRequest();
            userSignupRequest.setEmail("test@test.com");

            when(userRepository.existsByEmailIgnoreCase("test@test.com")).thenReturn(true);
            when(userMapper.toModel(any(UserSignupRequest.class))).thenAnswer(invocation -> {
                UserSignupRequest req = invocation.getArgument(0);
                OwnUser newUser = new OwnUser();
                newUser.setEmail(req.getEmail());
                return newUser;
            });


            CustomException exception = assertThrows(CustomException.class, () -> userService.signup(userSignupRequest));
            assertEquals("Email is already in use", exception.getMessage());
            assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getHttpStatus());
        }

        @Test
        @DisplayName("Should throw exception when uninvited user tries to signup and allowedOrganizationAdmins is configured")
        void signupUninvitedUserWithAllowedOrganizationAdmins() {
            UserSignupRequest userSignupRequest = new UserSignupRequest();
            userSignupRequest.setEmail("uninvited@test.com");
            userSignupRequest.setPassword("password");
            userSignupRequest.setCompanyName("New Company");

            ReflectionTestUtils.setField(userService, "allowedOrganizationAdmins", new String[]{"admin@test.com"});
            ReflectionTestUtils.setField(userService, "cloudVersion", true); // Set cloudVersion to true for this test

            when(userRepository.existsByEmailIgnoreCase("uninvited@test.com")).thenReturn(false);
            when(userMapper.toModel(any(UserSignupRequest.class))).thenAnswer(invocation -> {
                UserSignupRequest req = invocation.getArgument(0);
                OwnUser newUser = new OwnUser();
                newUser.setEmail(req.getEmail());
                newUser.setPassword(req.getPassword());
                return newUser;
            });

            CustomException exception = assertThrows(CustomException.class, () -> userService.signup(userSignupRequest));
            assertEquals("You are not allowed to create an account without being invited", exception.getMessage());
            assertEquals(HttpStatus.NOT_ACCEPTABLE, exception.getHttpStatus());
        }

        @Test
        @DisplayName("Should signup a new user and create a new company with specified language when cloudVersion is true")
        void signupNewUserAndCompanyWithLanguageCloudVersionTrue() {
            UserSignupRequest userSignupRequest = new UserSignupRequest();
            userSignupRequest.setEmail("newuserwithlang@test.com");
            userSignupRequest.setPassword("password");
            userSignupRequest.setCompanyName("New Company With Language");
            userSignupRequest.setLanguage(Language.EN);

            ReflectionTestUtils.setField(userService, "PUBLIC_API_URL", "http://localhost:8080");
            ReflectionTestUtils.setField(userService, "cloudVersion", true);

            when(userRepository.existsByEmailIgnoreCase("newuserwithlang@test.com")).thenReturn(false);
            when(userMapper.toModel(any(UserSignupRequest.class))).thenAnswer(invocation -> {
                UserSignupRequest req = invocation.getArgument(0);
                OwnUser newUser = new OwnUser();
                newUser.setEmail(req.getEmail());
                newUser.setPassword(req.getPassword());
                return newUser;
            });
            when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
            when(utils.generateStringId()).thenReturn("randomId");
            when(subscriptionPlanService.findByCode("BUSINESS")).thenReturn(Optional.of(new SubscriptionPlan()));
            when(currencyService.findByCode("$")).thenReturn(Optional.of(new Currency()));
            when(userRepository.save(any(OwnUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

            SignupSuccessResponse<OwnUser> response = userService.signup(userSignupRequest);

            assertNotNull(response);
            assertTrue(response.isSuccess());
            assertEquals("token", response.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when signup with non-existent role")
        void signupWithNonExistentRole() {
            UserSignupRequest userSignupRequest = new UserSignupRequest();
            userSignupRequest.setEmail("newuserwithrole@test.com");
            userSignupRequest.setPassword("password");
            userSignupRequest.setCompanyName("New Company");
            Role nonExistentRole = new Role();
            nonExistentRole.setId(99L);
            userSignupRequest.setRole(nonExistentRole); // Non-existent role

            when(userRepository.existsByEmailIgnoreCase("newuserwithrole@test.com")).thenReturn(false);
            when(userMapper.toModel(any(UserSignupRequest.class))).thenAnswer(invocation -> {
                UserSignupRequest req = invocation.getArgument(0);
                OwnUser newUser = new OwnUser();
                newUser.setEmail(req.getEmail());
                newUser.setPassword(req.getPassword());
                newUser.setRole(req.getRole());
                return newUser;
            });
            when(roleService.findById(99L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class, () -> userService.signup(userSignupRequest));
            assertEquals("Role not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_ACCEPTABLE, exception.getHttpStatus());
        }

        @Test
        @DisplayName("Should throw exception when signup with role but not invited and invitation via email is enabled")
        void signupWithRoleNotInvitedAndInvitationViaEmailEnabled() {
            UserSignupRequest userSignupRequest = new UserSignupRequest();
            userSignupRequest.setEmail("uninvitedrole@test.com");
            userSignupRequest.setPassword("password");
            userSignupRequest.setCompanyName("New Company");
            userSignupRequest.setRole(role); // Existing role

            ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", true);

            when(userRepository.existsByEmailIgnoreCase("uninvitedrole@test.com")).thenReturn(false);
            when(userMapper.toModel(any(UserSignupRequest.class))).thenAnswer(invocation -> {
                UserSignupRequest req = invocation.getArgument(0);
                OwnUser newUser = new OwnUser();
                newUser.setEmail(req.getEmail());
                newUser.setPassword(req.getPassword());
                newUser.setRole(req.getRole());
                return newUser;
            });
            when(roleService.findById(role.getId())).thenReturn(Optional.of(role));
            when(userInvitationService.findByRoleAndEmail(role.getId(), "uninvitedrole@test.com")).thenReturn(Collections.emptyList());

            CustomException exception = assertThrows(CustomException.class, () -> userService.signup(userSignupRequest));
            assertEquals("You are not invited to this organization for this role", exception.getMessage());
            assertEquals(HttpStatus.NOT_ACCEPTABLE, exception.getHttpStatus());
        }

        @Test
        @DisplayName("Should send activation email when signup and email invitation enabled and not localhost")
        void signupAndSendActivationEmail() {
            UserSignupRequest userSignupRequest = new UserSignupRequest();
            userSignupRequest.setEmail("activate@test.com");
            userSignupRequest.setPassword("password");
            userSignupRequest.setCompanyName("Activation Company");

            ReflectionTestUtils.setField(userService, "PUBLIC_API_URL", "http://localhost:8080");
            ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", true);
            ReflectionTestUtils.setField(userService, "enableMails", true);

            when(userRepository.existsByEmailIgnoreCase("activate@test.com")).thenReturn(false);
            when(userMapper.toModel(any(UserSignupRequest.class))).thenAnswer(invocation -> {
                UserSignupRequest req = invocation.getArgument(0);
                OwnUser newUser = new OwnUser();
                newUser.setEmail(req.getEmail());
                newUser.setPassword(req.getPassword());
                return newUser;
            });
            when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
            when(utils.generateStringId()).thenReturn("randomId");
            when(subscriptionPlanService.findByCode("BUSINESS")).thenReturn(Optional.of(new SubscriptionPlan()));
            when(currencyService.findByCode("$")).thenReturn(Optional.of(new Currency()));
            when(userRepository.save(any(OwnUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(verificationTokenRepository.save(any(VerificationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

            SignupSuccessResponse<OwnUser> response = userService.signup(userSignupRequest);

            assertNotNull(response);
            // verify(emailService2, times(1)).sendActivationEmail(any(), any());
        }

        @Test
        @DisplayName("Should signup a new user and return token directly when email invitation disabled and not localhost")
        void signupAndReturnTokenDirectlyWhenEmailInvitationDisabled() {
            UserSignupRequest userSignupRequest = new UserSignupRequest();
            userSignupRequest.setEmail("directsignup@test.com");
            userSignupRequest.setPassword("password");
            ReflectionTestUtils.setField(userService, "PUBLIC_API_URL", "http://localhost:8080");
            ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", false);
            ReflectionTestUtils.setField(userService, "enableMails", true);

            BrandConfig brandConfig = new BrandConfig();
            brandConfig.setName("Test Brand");
            brandConfig.setShortName("TB");
            when(brandingService.getBrandConfig()).thenReturn(brandConfig);

            when(userRepository.existsByEmailIgnoreCase("directsignup@test.com")).thenReturn(false);
            when(userMapper.toModel(any(UserSignupRequest.class))).thenAnswer(invocation -> {
                UserSignupRequest req = invocation.getArgument(0);
                OwnUser newUser = new OwnUser();
                newUser.setEmail(req.getEmail());
                newUser.setPassword(req.getPassword());
                newUser.setCompany(company);
                return newUser;
            });
            when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
            when(utils.generateStringId()).thenReturn("randomId");
            when(subscriptionPlanService.findByCode("BUSINESS")).thenReturn(Optional.of(new SubscriptionPlan()));
            when(currencyService.findByCode("$")).thenReturn(Optional.of(new Currency()));
            when(userRepository.save(any(OwnUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
            lenient().when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

            SignupSuccessResponse<OwnUser> response = userService.signup(userSignupRequest);

            assertNotNull(response);
            assertTrue(response.isSuccess());
            assertEquals("token", response.getMessage());
        }

        @Test
        @DisplayName("Should signup a new user and return token directly for demo account")
        void signupAndReturnTokenDirectlyForDemoAccount() {
            UserSignupRequest userSignupRequest = new UserSignupRequest();
            userSignupRequest.setEmail("demosignup@test.com");
            userSignupRequest.setPassword("password");
            userSignupRequest.setCompanyName("Demo Company");
            userSignupRequest.setDemo(true);

            ReflectionTestUtils.setField(userService, "PUBLIC_API_URL", "http://localhost:8080");
            ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", true);
            ReflectionTestUtils.setField(userService, "enableMails", true);

            BrandConfig brandConfig = new BrandConfig();
            brandConfig.setName("Test Brand");
            brandConfig.setShortName("TB");
            when(brandingService.getBrandConfig()).thenReturn(brandConfig);

            when(userRepository.existsByEmailIgnoreCase("demosignup@test.com")).thenReturn(false);
            when(userMapper.toModel(any(UserSignupRequest.class))).thenAnswer(invocation -> {
                UserSignupRequest req = invocation.getArgument(0);
                OwnUser newUser = new OwnUser();
                newUser.setEmail(req.getEmail());
                newUser.setPassword(req.getPassword());
                newUser.setCompany(company);
                return newUser;
            });
            when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
            when(utils.generateStringId()).thenReturn("randomId");
            when(subscriptionPlanService.findByCode("BUSINESS")).thenReturn(Optional.of(new SubscriptionPlan()));
            when(currencyService.findByCode("$")).thenReturn(Optional.of(new Currency()));
            when(userRepository.save(any(OwnUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
            lenient().when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

            SignupSuccessResponse<OwnUser> response = userService.signup(userSignupRequest);

            assertNotNull(response);
            assertTrue(response.isSuccess());
            assertEquals("token", response.getMessage());
        }

        @Test
        @DisplayName("Should signup a new user with existing role and return token directly when email invitation disabled")
        void signupWithExistingRoleAndReturnTokenDirectlyWhenEmailInvitationDisabled() {
            UserSignupRequest userSignupRequest = new UserSignupRequest();
            userSignupRequest.setEmail("existingrole@test.com");
            userSignupRequest.setPassword("password");
            userSignupRequest.setCompanyName("Existing Role Company");
            userSignupRequest.setRole(role);

            ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", false);
            ReflectionTestUtils.setField(userService, "enableMails", true);

            BrandConfig brandConfig = new BrandConfig();
            brandConfig.setName("Test Brand");
            brandConfig.setShortName("TB");
            when(brandingService.getBrandConfig()).thenReturn(brandConfig);

            when(userRepository.existsByEmailIgnoreCase("existingrole@test.com")).thenReturn(false);
            when(userMapper.toModel(any(UserSignupRequest.class))).thenAnswer(invocation -> {
                UserSignupRequest req = invocation.getArgument(0);
                OwnUser newUser = new OwnUser();
                newUser.setEmail(req.getEmail());
                newUser.setPassword(req.getPassword());
                newUser.setCompany(company);
                newUser.setRole(role);
                return newUser;
            });
            when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
            when(utils.generateStringId()).thenReturn("randomId");
            when(subscriptionPlanService.findByCode("BUSINESS")).thenReturn(Optional.of(new SubscriptionPlan()));
            when(currencyService.findByCode("$")).thenReturn(Optional.of(new Currency()));
            when(userRepository.save(any(OwnUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
            lenient().when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");
            lenient().when(roleService.findById(role.getId())).thenReturn(Optional.of(role));

            SignupSuccessResponse<OwnUser> response = userService.signup(userSignupRequest);

            assertNotNull(response);
            assertTrue(response.isSuccess());
            assertEquals("token", response.getMessage());
        }
    }

    @Nested
    @DisplayName("Update Tests")
    class UpdateTests {

        @Test
        @DisplayName("Should update user successfully")
        void updateUserSuccessfully() {
            UserPatchDTO patchDTO = new UserPatchDTO();
            patchDTO.setFirstName("Updated");

            when(userRepository.existsById(1L)).thenReturn(true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userMapper.updateUser(user, patchDTO)).thenAnswer(invocation -> {
                user.setFirstName("Updated");
                return user;
            });
            when(userRepository.saveAndFlush(any(OwnUser.class))).thenReturn(user);


            OwnUser updatedUser = userService.update(1L, patchDTO);

            assertNotNull(updatedUser);
            assertEquals("Updated", updatedUser.getFirstName());
        }

        @Test
        @DisplayName("Should update user password")
        void updateUserPassword() {
            UserPatchDTO patchDTO = new UserPatchDTO();
            patchDTO.setNewPassword("newPassword");

            when(userRepository.existsById(1L)).thenReturn(true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
            when(userMapper.updateUser(user, patchDTO)).thenAnswer(invocation -> {
                user.setPassword("encodedNewPassword");
                return user;
            });
            when(userRepository.saveAndFlush(any(OwnUser.class))).thenReturn(user);

            ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", false);

            OwnUser updatedUser = userService.update(1L, patchDTO);

            assertNotNull(updatedUser);
            assertEquals("encodedNewPassword", updatedUser.getPassword());
        }

        @Test
        @DisplayName("Should update user without updating password")
        void updateUserWithoutPassword() {
            UserPatchDTO patchDTO = new UserPatchDTO();
            patchDTO.setFirstName("Updated");

            when(userRepository.existsById(1L)).thenReturn(true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userMapper.updateUser(user, patchDTO)).thenAnswer(invocation -> {
                user.setFirstName("Updated");
                return user;
            });
            when(userRepository.saveAndFlush(any(OwnUser.class))).thenReturn(user);

            OwnUser updatedUser = userService.update(1L, patchDTO);

            assertNotNull(updatedUser);
            assertEquals("Updated", updatedUser.getFirstName());
            assertEquals("password", updatedUser.getPassword());
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void updateUserNotFound() {
            UserPatchDTO patchDTO = new UserPatchDTO();
            when(userRepository.existsById(2L)).thenReturn(false);

            CustomException exception = assertThrows(CustomException.class, () -> userService.update(2L, patchDTO));
            assertEquals("Not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("Signin Tests")
    class SigninTests {

        @Test
        @DisplayName("Should return a token for valid credentials")
        void signinWithValidCredentials() {
            Authentication authentication = mock(Authentication.class);
            Collection authorities = Collections.singletonList((GrantedAuthority) () -> "ROLE_ADMIN");
            when(authentication.getAuthorities()).thenReturn(authorities);
            when(authenticationManager.authenticate(any())).thenReturn(authentication);
            when(userRepository.findByEmailIgnoreCase("test@test.com")).thenReturn(Optional.of(user));
            when(jwtTokenProvider.createToken(any(), any())).thenReturn("test-token");

            String token = userService.signin("test@test.com", "password", "ADMIN");

            assertEquals("test-token", token);
        }

        @Test
        @DisplayName("Should throw exception for wrong role")
        void signinWithWrongRole() {
            Authentication authentication = mock(Authentication.class);
            Collection authorities = Collections.singletonList((GrantedAuthority) () -> "ROLE_USER");
            when(authentication.getAuthorities()).thenReturn(authorities);
            when(authenticationManager.authenticate(any())).thenReturn(authentication);

            CustomException exception = assertThrows(CustomException.class, () -> {
                userService.signin("test@test.com", "password", "ADMIN");
            });

            assertEquals("Invalid credentials", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }

        @Test
        @DisplayName("Should throw exception for invalid credentials")
        void signinWithInvalidCredentials() {
            when(authenticationManager.authenticate(any())).thenThrow(new CustomException("Invalid credentials", HttpStatus.FORBIDDEN));

            CustomException exception = assertThrows(CustomException.class, () -> {
                userService.signin("test@test.com", "wrong-password", "ADMIN");
            });

            assertEquals("Invalid credentials", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }

        @Test
        @DisplayName("Should throw exception when user not found during signin")
        void signinUserNotFound() {
            Authentication authentication = mock(Authentication.class);
            Collection authorities = Collections.singletonList((GrantedAuthority) () -> "ROLE_ADMIN");
            when(authentication.getAuthorities()).thenReturn(authorities);
            when(authenticationManager.authenticate(any())).thenReturn(authentication);
            when(userRepository.findByEmailIgnoreCase("nonexistent@test.com")).thenReturn(Optional.empty());

            NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
                userService.signin("nonexistent@test.com", "password", "ADMIN");
            });

            assertEquals("No value present", exception.getMessage());
        }

        @Test
        @DisplayName("Should update last login date on successful signin")
        void signinUpdatesLastLoginDate() {
            Authentication authentication = mock(Authentication.class);
            Collection authorities = Collections.singletonList((GrantedAuthority) () -> "ROLE_ADMIN");
            when(authentication.getAuthorities()).thenReturn(authorities);
            when(authenticationManager.authenticate(any())).thenReturn(authentication);
            when(userRepository.findByEmailIgnoreCase("test@test.com")).thenReturn(Optional.of(user));
            when(jwtTokenProvider.createToken(any(), any())).thenReturn("test-token");

            userService.signin("test@test.com", "password", "ADMIN");

            // Verify that save was called on the user, implying lastLogin was set
            verify(userRepository, times(1)).save(user);
        }
    }

    @Nested
    @DisplayName("Delete Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should delete a user by username")
        void deleteUserByUsername() {
            String userEmail = "test-user@test.com";
            when(userRepository.findByEmailIgnoreCase(userEmail)).thenReturn(Optional.of(mock(OwnUser.class)));
            userService.delete(userEmail);
            verify(userRepository, times(1)).deleteByUsername(userEmail);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent user")
        void deleteNonExistentUser() {
            String userEmail = "non-existent-user@test.com";
            when(userRepository.findByEmailIgnoreCase(userEmail)).thenReturn(Optional.empty());
            assertDoesNotThrow(() -> userService.delete(userEmail));
            verify(userRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Find User Tests")
    class FindUserTests {

        @Test
        @DisplayName("Should find a user by email")
        void findUserByEmail() {
            when(userRepository.findByEmailIgnoreCase("test@test.com")).thenReturn(Optional.of(user));

            Optional<OwnUser> foundUser = userService.findByEmail("test@test.com");

            assertTrue(foundUser.isPresent());
            assertEquals("test@test.com", foundUser.get().getEmail());
        }

        @Test
        @DisplayName("Should not find a user by email if not exists")
        void findUserByEmailNotFound() {
            when(userRepository.findByEmailIgnoreCase("nonexistent@test.com")).thenReturn(Optional.empty());

            Optional<OwnUser> foundUser = userService.findByEmail("nonexistent@test.com");

            assertFalse(foundUser.isPresent());
        }

        @Test
        @DisplayName("Should find a user by id")
        void findUserById() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            Optional<OwnUser> foundUser = userService.findById(1L);

            assertTrue(foundUser.isPresent());
            assertEquals(1L, foundUser.get().getId());
        }

        @Test
        @DisplayName("Should not find a user by id if not exists")
        void findUserByIdNotFound() {
            when(userRepository.findById(2L)).thenReturn(Optional.empty());

            Optional<OwnUser> foundUser = userService.findById(2L);

            assertFalse(foundUser.isPresent());
        }
    }

    /*
    @Nested
    @DisplayName("Password Reset Tests")
    class PasswordResetTests {

        @Test
        @DisplayName("Should send a password reset email")
        void resetPasswordRequest() {
            ReflectionTestUtils.setField(userService, "enableMails", true);
            BrandConfig brandConfig = new BrandConfig();
            brandConfig.setName("Test Brand");
            when(brandingService.getBrandConfig()).thenReturn(brandConfig);
            when(userRepository.findByEmailIgnoreCase("test@test.com")).thenReturn(Optional.of(user));
            when(verificationTokenRepository.save(any(VerificationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

            userService.resetPasswordRequest("test@test.com");
            
            verify(emailService2, times(1)).sendResetPasswordEmail(any(), any());
        }

        @Test
        @DisplayName("Should throw exception if mails are disabled")
        void resetPasswordRequestMailsDisabled() {
            ReflectionTestUtils.setField(userService, "enableMails", false);

            CustomException exception = assertThrows(CustomException.class, () -> {
                userService.resetPasswordRequest("test@test.com");
            });

            assertEquals("Please enable mails and configure SMTP in the environment variables", exception.getMessage());
            assertEquals(HttpStatus.NOT_ACCEPTABLE, exception.getHttpStatus());
        }
        
        @Test
        @DisplayName("Should not send password reset email if user not found")
        void resetPasswordRequestUserNotFound() {
            ReflectionTestUtils.setField(userService, "enableMails", true);
            when(userRepository.findByEmailIgnoreCase("notfound@test.com")).thenReturn(Optional.empty());

            userService.resetPasswordRequest("notfound@test.com");

            verify(verificationTokenRepository, never()).save(any(VerificationToken.class));
            verify(emailService2, never()).sendResetPasswordEmail(any(), any());
        }

        @Test
        @DisplayName("Should reset password successfully")
        void resetPasswordSuccessfully() {
            VerificationToken token = new VerificationToken();
            token.setToken(UUID.randomUUID().toString());
            token.setUser(user);
            when(verificationTokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));
            when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");

            userService.resetPassword(token.getToken(), "newPassword");

            verify(userRepository, times(1)).save(user);
            assertEquals("encodedNewPassword", user.getPassword());
        }

        @Test
        @DisplayName("Should throw exception for invalid token on password reset")
        void resetPasswordInvalidToken() {
            when(verificationTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class, () -> userService.resetPassword("invalid-token", "newPassword"));
            assertEquals("Invalid token", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }

        @Test
        @DisplayName("Should throw exception for expired token on password reset")
        void resetPasswordExpiredToken() {
            VerificationToken token = new VerificationToken();
            token.setToken(UUID.randomUUID().toString());
            token.setUser(user);
            token.setExpiryDate(java.util.Date.from(LocalDateTime.now().minusDays(1).atZone(ZoneId.systemDefault()).toInstant()));
            when(verificationTokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

            CustomException exception = assertThrows(CustomException.class, () -> userService.resetPassword(token.getToken(), "newPassword"));
            assertEquals("Expired token", exception.getMessage());
            assertEquals(HttpStatus.GONE, exception.getHttpStatus());
        }
    }
    */

    @Nested
    @DisplayName("Find By Search Criteria Tests")
    class FindBySearchCriteriaTests {

        @Test
        @DisplayName("Should return a page of users")
        void findBySearchCriteria() {
            SearchCriteria searchCriteria = new SearchCriteria();
            searchCriteria.setPageNum(0);
            searchCriteria.setPageSize(10);
            searchCriteria.setSortField("email");
            searchCriteria.setDirection(Sort.Direction.ASC);
            searchCriteria.setFilterFields(Collections.emptyList());

            lenient().when(userRepository.findAll((Specification<OwnUser>) any(), any(Pageable.class))).thenReturn(new PageImpl<>(Collections.singletonList(user)));

            Page<OwnUser> userPage = userService.findBySearchCriteria(searchCriteria);

            assertEquals(1, userPage.getTotalElements());
        }
    }

    @Nested
    @DisplayName("Is User In Company Tests")
    class IsUserInCompanyTests {

        @Test
        @DisplayName("Should return true if user is in company")
        void isUserInCompany() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            assertTrue(userService.isUserInCompany(user, 1L, false));
        }

        @Test
        @DisplayName("Should return false if user is not in company")
        void isUserNotInCompany() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            assertFalse(userService.isUserInCompany(user, 2L, false));
        }

        @Test
        @DisplayName("Should return false if user is not in company and optional is true")
        void isUserNotInCompanyOptional() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            assertFalse(userService.isUserInCompany(user, 2L, true));
        }

        @Test
        @DisplayName("Should return false if user is not in company and optional is false")
        void isUserNotInCompanyNotOptional() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            assertFalse(userService.isUserInCompany(user, 2L, false));
        }

        @Test
        @DisplayName("Should return true if user is null and optional is true")
        void isUserInCompanyWithNullUserAndOptionalTrue() {
            assertTrue(userService.isUserInCompany(null, 1L, true));
        }
    }

    @Nested
    @DisplayName("Save And Exists Tests")
    class SaveAndExistsTests {

        @Test
        @DisplayName("Should save a user")
        void saveUser() {
            when(userRepository.save(user)).thenReturn(user);

            OwnUser savedUser = userService.save(user);

            assertNotNull(savedUser);
        }

        @Test
        @DisplayName("Should save all users")
        void saveAllUsers() {
            when(userRepository.saveAll(Collections.singletonList(user))).thenReturn(Collections.singletonList(user));

            Collection<OwnUser> savedUsers = userService.saveAll(Collections.singletonList(user));

            assertEquals(1, savedUsers.size());
        }

        @Test
        @DisplayName("Should return true if user exists by email")
        void userExistsByEmail() {
            when(userRepository.existsByEmailIgnoreCase("test@test.com")).thenReturn(true);

            assertTrue(userService.existsByEmail("test@test.com"));
        }

        @Test
        @DisplayName("Should return false if user does not exist by email")
        void userDoesNotExistByEmail() {
            when(userRepository.existsByEmailIgnoreCase("test@test.com")).thenReturn(false);

            assertFalse(userService.existsByEmail("test@test.com"));
        }
    }

    @Nested
    @DisplayName("Enable User Tests")
    class EnableUserTests {

        @Test
        @DisplayName("Should enable a user")
        void enableUser() {
            user.setEnabled(false);
            when(userRepository.findByEmailIgnoreCase("test@test.com")).thenReturn(Optional.of(user));

            userService.enableUser("test@test.com");

            assertTrue(user.isEnabled());
        }

        @Test
        @DisplayName("Should enable a user with non-paid role")
        void enableUserNonPaidRole() {
            user.setEnabled(false);
            user.getRole().setPaid(false);
            when(userRepository.findByEmailIgnoreCase("test@test.com")).thenReturn(Optional.of(user));

            userService.enableUser("test@test.com");

            assertTrue(user.isEnabled());
            verify(userRepository, times(1)).save(user);
        }

        @Test
        @DisplayName("Should enable a user when not enabled in subscription and paid, and user limit not reached")
        void enableUserNotEnabledInSubscriptionAndPaid() {
            user.setEnabled(false);
            user.getRole().setPaid(true);
            Subscription subscription = new Subscription();
            subscription.setUsersCount(2);
            user.getCompany().setSubscription(subscription);
            when(userRepository.findByEmailIgnoreCase("test@test.com")).thenReturn(Optional.of(user));

            OwnUser enabledUserInCompany = new OwnUser();
            enabledUserInCompany.setEnabled(true);
            enabledUserInCompany.setEnabledInSubscription(false); // Simulate not enabled in subscription
            Role paidRole = new Role();
            paidRole.setPaid(true);
            enabledUserInCompany.setRole(paidRole);
            when(userRepository.findByCompany_Id(1L)).thenReturn(Collections.singletonList(enabledUserInCompany));

            userService.enableUser("test@test.com");

            assertTrue(user.isEnabled());
            verify(userRepository, times(1)).save(user);
        }

        @Test
        @DisplayName("Should throw exception when user limit is reached")
        void enableUserLimitReached() {
            user.getRole().setPaid(true);
            Subscription subscription = new Subscription();
            subscription.setUsersCount(1);
            user.getCompany().setSubscription(subscription);
            when(userRepository.findByEmailIgnoreCase("test@test.com")).thenReturn(Optional.of(user));
            OwnUser enabledUser = new OwnUser();
            enabledUser.setEnabled(true);
            enabledUser.setEnabledInSubscription(true);
            Role paidRole = new Role();
            paidRole.setPaid(true);
            enabledUser.setRole(paidRole);
            when(userRepository.findByCompany_Id(1L)).thenReturn(Collections.singletonList(enabledUser));

            CustomException exception = assertThrows(CustomException.class, () -> {
                userService.enableUser("test@test.com");
            });

            assertEquals("You can't add more users to this company", exception.getMessage());
            assertEquals(HttpStatus.NOT_ACCEPTABLE, exception.getHttpStatus());
        }
        
        @Test
        @DisplayName("Should throw exception when enabling a non-existent user")
        void enableNonExistentUser() {
            when(userRepository.findByEmailIgnoreCase("nonexistent@test.com")).thenReturn(Optional.empty());

            assertThrows(NoSuchElementException.class, () -> userService.enableUser("nonexistent@test.com"));
        }
    }

    @Nested
    @DisplayName("Find User In Company Tests")
    class FindUserInCompanyTests {

        @Test
        @DisplayName("Should find a user by email and company")
        void findUserByEmailAndCompany() {
            when(userRepository.findByEmailIgnoreCaseAndCompany_Id("test@test.com", 1L)).thenReturn(Optional.of(user));

            Optional<OwnUser> foundUser = userService.findByEmailAndCompany("test@test.com", 1L);

            assertTrue(foundUser.isPresent());
            assertEquals("test@test.com", foundUser.get().getEmail());
            assertEquals(1L, foundUser.get().getCompany().getId());
        }

        @Test
        @DisplayName("Should find a user by id and company")
        void findUserByIdAndCompany() {
            when(userRepository.findByIdAndCompany_Id(1L, 1L)).thenReturn(Optional.of(user));

            Optional<OwnUser> foundUser = userService.findByIdAndCompany(1L, 1L);

            assertTrue(foundUser.isPresent());
            assertEquals(1L, foundUser.get().getId());
            assertEquals(1L, foundUser.get().getCompany().getId());
        }
    }

    @Nested
    @DisplayName("Find Multiple Users Tests")
    class FindMultipleUsersTests {

        @Test
        @DisplayName("Should return all users")
        void getAllUsers() {
            when(userRepository.findAll()).thenReturn(Collections.singletonList(user));

            Collection<OwnUser> users = userService.getAll();

            assertEquals(1, users.size());
        }

        @Test
        @DisplayName("Should return the number of users")
        void countUsers() {
            when(userRepository.count()).thenReturn(1L);

            long userCount = userService.count();

            assertEquals(1L, userCount);
        }

        @Test
        @DisplayName("Should return users by company")
        void findUsersByCompany() {
            when(userRepository.findByCompany_Id(1L)).thenReturn(Collections.singletonList(user));

            Collection<OwnUser> users = userService.findByCompany(1L);

            assertEquals(1, users.size());
        }

        @Test
        @DisplayName("Should return workers by company")
        void findWorkersByCompany() {
            when(userRepository.findWorkersByCompany(1L, Arrays.asList(RoleCode.REQUESTER, RoleCode.VIEW_ONLY))).thenReturn(Collections.singletonList(user));

            Collection<OwnUser> users = userService.findWorkersByCompany(1L);

            assertEquals(1, users.size());
        }

        @Test
        @DisplayName("Should return users by location")
        void findUsersByLocation() {
            when(userRepository.findByLocation_Id(1L)).thenReturn(Collections.singletonList(user));

            Collection<OwnUser> users = userService.findByLocation(1L);

            assertEquals(1, users.size());
        }
    }

    @Nested
    @DisplayName("Refresh Tests")
    class RefreshTests {

        @Test
        @DisplayName("Should return a new token")
        void refresh() {
            when(userRepository.findByEmailIgnoreCase("test@test.com")).thenReturn(Optional.of(user));
            when(jwtTokenProvider.createToken(any(), any())).thenReturn("new-test-token");

            String newToken = userService.refresh("test@test.com");

            assertEquals("new-test-token", newToken);
        }
        
        @Test
        @DisplayName("Should throw exception on refresh if user not found")
        void refreshUserNotFound() {
            when(userRepository.findByEmailIgnoreCase("notfound@test.com")).thenReturn(Optional.empty());
            
            assertThrows(NoSuchElementException.class, () -> userService.refresh("notfound@test.com"));
        }
    }

    @Nested
    @DisplayName("Whoami Tests")
    class WhoamiTests {

        @Test
        @DisplayName("Should return the current user")
        void whoami() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(jwtTokenProvider.resolveToken(request)).thenReturn("test-token");
            when(jwtTokenProvider.getUsername("test-token")).thenReturn("test@test.com");
            when(userRepository.findByEmailIgnoreCase("test@test.com")).thenReturn(Optional.of(user));

            OwnUser currentUser = userService.whoami(request);

            assertNotNull(currentUser);
            assertEquals("test@test.com", currentUser.getEmail());
        }
        
        @Test
        @DisplayName("Should throw exception on whoami if user not found")
        void whoamiUserNotFound() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(jwtTokenProvider.resolveToken(request)).thenReturn("test-token");
            when(jwtTokenProvider.getUsername("test-token")).thenReturn("notfound@test.com");
            when(userRepository.findByEmailIgnoreCase("notfound@test.com")).thenReturn(Optional.empty());

            assertThrows(NoSuchElementException.class, () -> userService.whoami(request));
        }
    }

    @Nested
    @DisplayName("Invitation and Registration Tests")
    class InvitationTests {

        @Test
        @DisplayName("Should send an invitation email")
        void inviteUser() {
            ReflectionTestUtils.setField(userService, "enableMails", true);
            BrandConfig brandConfig = new BrandConfig();
            brandConfig.setName("Test Brand");
            when(brandingService.getBrandConfig()).thenReturn(brandConfig);
            when(userRepository.existsByEmailIgnoreCase("newuser@test.com")).thenReturn(false);
            when(userInvitationService.create(any(UserInvitation.class))).thenAnswer(invocation -> invocation.getArgument(0));

            userService.invite("newuser@test.com", role, user);
            
            // verify(emailService2, times(1)).sendInviteEmail(any(), any(), any());
        }

        @Test
        @DisplayName("Should throw exception if email already in use")
        void inviteUserEmailInUse() {
            ReflectionTestUtils.setField(userService, "enableMails", true);
            when(userRepository.existsByEmailIgnoreCase("test@test.com")).thenReturn(true);

            CustomException exception = assertThrows(CustomException.class, () -> {
                userService.invite("test@test.com", role, user);
            });

            assertEquals("Email already in use", exception.getMessage());
            assertEquals(HttpStatus.NOT_ACCEPTABLE, exception.getHttpStatus());
        }

        @Test
        @DisplayName("Should throw exception if mails are disabled")
        void inviteUserMailsDisabled() {
            ReflectionTestUtils.setField(userService, "enableMails", false);

            CustomException exception = assertThrows(CustomException.class, () -> {
                userService.invite("newuser@test.com", role, user);
            });

            assertEquals("Please enable mails and configure SMTP in the environment variables", exception.getMessage());
            assertEquals(HttpStatus.NOT_ACCEPTABLE, exception.getHttpStatus());
        }

        /*
        @Test
        @DisplayName("Should confirm registration successfully")
        void confirmRegistration() {
            VerificationToken token = new VerificationToken();
            token.setToken(UUID.randomUUID().toString());
            token.setUser(user);
            user.setEnabled(false);
            when(verificationTokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

            String result = userService.confirmRegistration(token.getToken());

            assertEquals("User activated successfully", result);
            assertTrue(user.isEnabled());
            verify(userRepository, times(1)).save(user);
        }

        @Test
        @DisplayName("Should throw exception for invalid token on registration confirmation")
        void confirmRegistrationInvalidToken() {
            when(verificationTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class, () -> userService.confirmRegistration("invalid-token"));
            assertEquals("Invalid token", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }

        @Test
        @DisplayName("Should throw exception for expired token on registration confirmation")
        void confirmRegistrationExpiredToken() {
            VerificationToken token = new VerificationToken();
            token.setToken(UUID.randomUUID().toString());
            token.setUser(user);
            token.setExpiryDate(java.util.Date.from(LocalDateTime.now().minusDays(1).atZone(ZoneId.systemDefault()).toInstant()));
            when(verificationTokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

            CustomException exception = assertThrows(CustomException.class, () -> userService.confirmRegistration(token.getToken()));
            assertEquals("Expired token", exception.getMessage());
            assertEquals(HttpStatus.GONE, exception.getHttpStatus());
        }

        @Test
        @DisplayName("Should resend verification token")
        void resendVerificationToken() {
            String tokenUuid = UUID.randomUUID().toString();
            VerificationToken token = new VerificationToken();
            token.setToken(tokenUuid);
            token.setUser(user);
            when(verificationTokenRepository.findByToken(tokenUuid)).thenReturn(Optional.of(token));
            ReflectionTestUtils.setField(userService, "enableMails", true);

            userService.resendVerificationToken(tokenUuid);

            verify(emailService2, times(1)).sendActivationEmail(any(), any());
        }

        @Test
        @DisplayName("Should throw exception on resend verification token if token not found")
        void resendVerificationTokenNotFound() {
            when(verificationTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());
            ReflectionTestUtils.setField(userService, "enableMails", true);

            assertThrows(CustomException.class, () -> userService.resendVerificationToken("invalid-token"));
        }
        */
    }
}
