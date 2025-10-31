package com.grash.service;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.BrandConfig;
import com.grash.dto.SignupSuccessResponse;
import com.grash.dto.UserPatchDTO;
import com.grash.dto.UserSignupRequest;
import com.grash.exception.CustomException;
import com.grash.mapper.UserMapper;
import com.grash.model.*;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
            userSignupRequest.setCompanyName("New Company");

            ReflectionTestUtils.setField(userService, "PUBLIC_API_URL", "http://remotehost");
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
    }

    @Nested
    @DisplayName("Delete Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should delete a user by username")
        void deleteUserByUsername() {
            userService.delete("test-user");
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
    }

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
        void isUserInCompanyOptional() {
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
    }

    @Nested
    @DisplayName("Invitation Tests")
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
    }
}
