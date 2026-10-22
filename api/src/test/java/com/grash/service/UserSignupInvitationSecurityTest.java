package com.grash.service;

import com.grash.dto.UserSignupRequest;
import com.grash.dto.SignupSuccessResponse;
import com.grash.exception.CustomException;
import com.grash.mapper.UserMapper;
import com.grash.model.Company;
import com.grash.model.CompanySettings;
import com.grash.model.Role;
import com.grash.model.User;
import com.grash.model.UserInvitation;
import com.grash.model.enums.RoleType;
import com.grash.repository.UserRepository;
import com.grash.security.JwtTokenProvider;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSignupInvitationSecurityTest {
    @InjectMocks
    private UserService userService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RoleService roleService;
    @Mock
    private UserInvitationService userInvitationService;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private EntityManager em;
    @Mock
    private CompanyService companyService;
    @Mock
    private SubscriptionService subscriptionService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "recipients", new String[]{});
        ReflectionTestUtils.setField(userService, "allowedOrganizationAdmins", new String[]{});
    }

    @ParameterizedTest(name = "custom role without invitation is rejected when INVITATION_VIA_EMAIL={0}")
    @ValueSource(booleans = {false, true})
    void customCompanyRoleWithoutInvitation_cannotCreateAccountOrSession(boolean invitationViaEmail) {
        ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", invitationViaEmail);
        Company existingCompany = new Company();
        existingCompany.setId(42L);
        CompanySettings settings = new CompanySettings();
        settings.setCompany(existingCompany);
        Role customRole = Role.builder().id(81L).name("Company observer")
                .roleType(RoleType.ROLE_CLIENT).paid(false).companySettings(settings).build();
        UserSignupRequest request = UserSignupRequest.builder()
                .email("uninvited@example.com").password("UninvitedPassword123!")
                .firstName("Uninvited").lastName("User").phone("12345678")
                .role(customRole).build();
        User mappedUser = new User();
        mappedUser.setEmail(request.getEmail());
        mappedUser.setPassword(request.getPassword());
        mappedUser.setRole(customRole);
        lenient().when(userMapper.toModel(request)).thenReturn(mappedUser);
        lenient().when(roleService.findById(customRole.getId())).thenReturn(Optional.of(customRole));
        lenient().when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");

        assertAll(
                () -> assertThrows(CustomException.class, () -> userService.signup(request)),
                () -> verify(userRepository, never()).save(any(User.class)),
                () -> verify(userRepository, never()).saveAndFlush(any(User.class)),
                () -> verify(jwtTokenProvider, never()).createToken(anyString(), anyList()),
                () -> verifyNoInteractions(refreshTokenService)
        );
    }

    private UserSignupRequest requestWithToken(String token) {
        return UserSignupRequest.builder().email("invited@example.com").password("InvitedPassword123!")
                .firstName("Invited").lastName("User").phone("12345678")
                .invitationToken(token).build();
    }

    private UserInvitation validInvitation() {
        Company company = new Company();
        company.setId(42L);
        Role role = Role.builder().id(81L).name("Company observer")
                .roleType(RoleType.ROLE_CLIENT).paid(false).build();
        UserInvitation invitation = new UserInvitation("invited@example.com", role);
        invitation.setCompany(company);
        return invitation;
    }

    private User prepareMappedUser(UserSignupRequest request) {
        User user = new User();
        user.setEmail(request.getEmail().trim().toLowerCase(java.util.Locale.ROOT));
        user.setPassword(request.getPassword());
        when(userMapper.toModel(request)).thenReturn(user);
        lenient().when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");
        return user;
    }

    private void assertNoAccountOrSessionCreated() {
        verify(userRepository, never()).save(any(User.class));
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verifyNoInteractions(jwtTokenProvider, refreshTokenService, companyService, subscriptionService);
        verify(userInvitationService, never()).consume(any(), any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void companyIdWithoutInvitation_isRejected(boolean invitationViaEmail) {
        ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", invitationViaEmail);
        UserSignupRequest request = requestWithToken(null);
        request.setCompanyId(42L);

        assertThrows(CustomException.class, () -> userService.signup(request));

        assertNoAccountOrSessionCreated();
    }

    static Stream<Arguments> rejectedTokens() {
        return Stream.of(false, true).flatMap(email ->
                Stream.of("invalid", "expired", "consumed", "", " ")
                        .map(token -> Arguments.of(email, token)));
    }

    @ParameterizedTest(name = "INVITATION_VIA_EMAIL={0}, token={1}")
    @MethodSource("rejectedTokens")
    void rejectedInvitationNeverCreatesAccountOrSession(boolean invitationViaEmail, String token) {
        ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", invitationViaEmail);
        UserSignupRequest request = requestWithToken(token);
        when(userInvitationService.lockValidInvitation(token))
                .thenThrow(new CustomException("Invalid or expired invitation", HttpStatus.NOT_ACCEPTABLE));

        CustomException failure = assertThrows(CustomException.class, () -> userService.signup(request));

        assertEquals(HttpStatus.NOT_ACCEPTABLE, failure.getHttpStatus());
        assertNoAccountOrSessionCreated();
        verifyNoInteractions(userMapper);
    }

    static Stream<Arguments> mismatchedClaims() {
        return Stream.of(false, true).flatMap(email ->
                Stream.of("email", "role", "company", "roleWithoutId")
                        .map(claim -> Arguments.of(email, claim)));
    }

    @ParameterizedTest(name = "INVITATION_VIA_EMAIL={0}, mismatched {1}")
    @MethodSource("mismatchedClaims")
    void invitationClaimsCannotBeReplaced(boolean invitationViaEmail, String claim) {
        ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", invitationViaEmail);
        UserSignupRequest request = requestWithToken("valid-token");
        switch (claim) {
            case "email" -> request.setEmail("attacker@example.com");
            case "role" -> request.setRole(Role.builder().id(99L).build());
            case "company" -> request.setCompanyId(99L);
            case "roleWithoutId" -> request.setRole(new Role());
        }
        when(userInvitationService.lockValidInvitation("valid-token")).thenReturn(validInvitation());

        CustomException failure = assertThrows(CustomException.class, () -> userService.signup(request));

        assertEquals(HttpStatus.NOT_ACCEPTABLE, failure.getHttpStatus());
        assertNoAccountOrSessionCreated();
        verifyNoInteractions(userMapper);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void publicSignupRejectsSuperAdministratorEvenWithInvitation(boolean invitationViaEmail) {
        ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", invitationViaEmail);
        UserInvitation invitation = validInvitation();
        invitation.getRole().setRoleType(RoleType.ROLE_SUPER_ADMIN);
        when(userInvitationService.lockValidInvitation("valid-token")).thenReturn(invitation);

        CustomException failure = assertThrows(CustomException.class,
                () -> userService.signup(requestWithToken("valid-token")));

        assertEquals(HttpStatus.FORBIDDEN, failure.getHttpStatus());
        assertNoAccountOrSessionCreated();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void validInvitationUsesAuthoritativeClaimsAndIsConsumedBeforeSession(boolean invitationViaEmail) {
        ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", invitationViaEmail);
        ReflectionTestUtils.setField(userService, "allowedOrganizationAdmins", new String[]{"owner@example.com"});
        UserSignupRequest request = requestWithToken("valid-token");
        request.setEmail("  INVITED@EXAMPLE.COM  ");
        request.setCompanyName("Attacker-selected company name");
        UserInvitation invitation = validInvitation();
        // A mapper or client-supplied object must not override any invitation claim.
        User mappedUser = prepareMappedUser(request);
        mappedUser.setRole(Role.builder().id(99L).roleType(RoleType.ROLE_SUPER_ADMIN).build());
        Company otherCompany = new Company();
        otherCompany.setId(99L);
        mappedUser.setCompany(otherCompany);
        mappedUser.setOwnsCompany(true);
        when(userInvitationService.lockValidInvitation("valid-token")).thenReturn(invitation);
        when(jwtTokenProvider.createToken(anyString(), anyList())).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(mappedUser)).thenReturn("refresh-token");

        SignupSuccessResponse<User> response = userService.signup(request);

        assertTrue(response.isSuccess());
        assertEquals("access-token", response.getMessage());
        assertEquals("refresh-token", response.getRefreshToken());
        assertSame(invitation.getCompany(), response.getUser().getCompany());
        assertSame(invitation.getRole(), response.getUser().getRole());
        assertEquals(invitation.getEmail(), response.getUser().getEmail());
        assertFalse(response.getUser().isOwnsCompany());
        assertTrue(response.getUser().isEnabled());
        InOrder order = inOrder(userInvitationService, userRepository, em, jwtTokenProvider, refreshTokenService);
        order.verify(userInvitationService).lockValidInvitation("valid-token");
        order.verify(userRepository).existsByEmailIgnoreCase("invited@example.com");
        order.verify(userRepository).saveAndFlush(mappedUser);
        order.verify(userInvitationService).consume(invitation, mappedUser);
        order.verify(em).flush();
        order.verify(jwtTokenProvider).createToken(eq(invitation.getEmail()), anyList());
        order.verify(refreshTokenService).createRefreshToken(mappedUser);
        verifyNoInteractions(companyService, subscriptionService);
    }

    @Test
    void matchingLegacyAssertionsDoNotSupplyTheRoleOrCompanyObjects() {
        UserSignupRequest request = requestWithToken("valid-token");
        Role clientRole = Role.builder().id(81L).roleType(RoleType.ROLE_SUPER_ADMIN).build();
        request.setRole(clientRole);
        request.setCompanyId(42L);
        UserInvitation invitation = validInvitation();
        User user = prepareMappedUser(request);
        when(userInvitationService.lockValidInvitation("valid-token")).thenReturn(invitation);

        userService.signup(request);

        assertSame(invitation.getRole(), user.getRole());
        assertSame(invitation.getCompany(), user.getCompany());
        assertNotSame(clientRole, user.getRole());
    }

    @Test
    void existingAccountDoesNotConsumeInvitationOrCreateSession() {
        UserSignupRequest request = requestWithToken("valid-token");
        prepareMappedUser(request);
        when(userInvitationService.lockValidInvitation("valid-token")).thenReturn(validInvitation());
        when(userRepository.existsByEmailIgnoreCase("invited@example.com")).thenReturn(true);

        CustomException failure = assertThrows(CustomException.class, () -> userService.signup(request));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, failure.getHttpStatus());
        assertNoAccountOrSessionCreated();
    }

    @Test
    void failedAccountPersistenceDoesNotConsumeInvitationOrCreateSession() {
        UserSignupRequest request = requestWithToken("valid-token");
        prepareMappedUser(request);
        when(userInvitationService.lockValidInvitation("valid-token")).thenReturn(validInvitation());
        when(userRepository.saveAndFlush(any())).thenThrow(new IllegalStateException("database unavailable"));

        assertThrows(IllegalStateException.class, () -> userService.signup(request));

        verify(userInvitationService, never()).consume(any(), any());
        verifyNoInteractions(jwtTokenProvider, refreshTokenService);
    }

    @Test
    void failedConsumptionFlushDoesNotCreateSession() {
        UserSignupRequest request = requestWithToken("valid-token");
        prepareMappedUser(request);
        when(userInvitationService.lockValidInvitation("valid-token")).thenReturn(validInvitation());
        doThrow(new IllegalStateException("audit write failed")).when(em).flush();

        assertThrows(IllegalStateException.class, () -> userService.signup(request));

        verifyNoInteractions(jwtTokenProvider, refreshTokenService);
    }
}
