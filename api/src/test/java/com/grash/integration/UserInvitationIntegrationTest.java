package com.grash.integration;

import com.grash.dto.IssuedInvitationDTO;
import com.grash.dto.SignupSuccessResponse;
import com.grash.dto.UserSignupRequest;
import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleCode;
import com.grash.model.enums.RoleType;
import com.grash.repository.*;
import com.grash.service.RefreshTokenService;
import com.grash.service.UserInvitationService;
import com.grash.service.UserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;

/** Real PostgreSQL locks/transactions: unit mocks cannot establish these guarantees. */
class UserInvitationIntegrationTest extends AbstractIntegrationTest {
    @Autowired private UserRepository users;
    @Autowired private UserInvitationRepository invitations;
    @Autowired private RefreshTokenRepository refreshTokens;
    @Autowired private CompanyRepository companies;
    @Autowired private RoleRepository roles;
    @Autowired private CurrencyRepository currencies;
    @Autowired private EntityManager em;
    @Autowired private UserInvitationService invitationService;
    @Autowired private UserService userService;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoSpyBean private RefreshTokenService refreshTokenService;

    private TransactionTemplate transaction;
    private Long companyId;
    private Long roleId;
    private Long inviterId;
    private String invitedEmail;
    private String token;

    @BeforeEach
    void prepareInvitation() {
        transaction = new TransactionTemplate(transactionManager);
        invitedEmail = "invited-" + UUID.randomUUID() + "@example.com";
        transaction.executeWithoutResult(status -> {
            Company company = new Company();
            company.setName("Invitation security test");
            company.getCompanySettings().getGeneralPreferences().setCurrency(currencies.findFirstBy().orElseThrow());
            company.getCompanySettings().getGeneralPreferences().setTimeZone("UTC");
            company = companies.saveAndFlush(company);
            companyId = company.getId();
            Role role = roles.save(Role.builder().name("Invitation test role").roleType(RoleType.ROLE_CLIENT)
                    .code(RoleCode.USER_CREATED).paid(false).companySettings(company.getCompanySettings())
                    .createPermissions(Set.of(PermissionEntity.PEOPLE_AND_TEAMS)).build());
            roleId = role.getId();
            User inviter = new User();
            inviter.setEmail("inviter-" + UUID.randomUUID() + "@example.com");
            inviter.setFirstName("Invitation");
            inviter.setLastName("Issuer");
            inviter.setUsername(UUID.randomUUID().toString());
            inviter.setPassword("already-encoded");
            inviter.setEnabled(true);
            inviter.setCompany(company);
            inviter.setRole(role);
            inviter = users.saveAndFlush(inviter);
            inviterId = inviter.getId();
            IssuedInvitationDTO issued = invitationService.issue(invitedEmail, role, inviter);
            token = issued.getInvitationLink().split("#invitationToken=", 2)[1];
            em.flush();
            em.clear();
        });
    }

    @AfterEach
    void cleanUp() {
        reset(refreshTokenService);
        ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", false);
        transaction.executeWithoutResult(status -> {
            // Limit cleanup to this test's tenant; the shared test database also holds bootstrap data.
            companies.findById(companyId).ifPresent(companies::delete);
            em.flush();
            em.clear();
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void concurrentAcceptances_commitOnlyOneAccountOneSessionAndOneAudit(boolean emailDelivery) throws Exception {
        ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", emailDelivery);
        long sessionsBefore = refreshTokens.count();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<SignupSuccessResponse<User>>> attempts = new ArrayList<>();
        try {
            for (int i = 0; i < 2; i++) {
                attempts.add(executor.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(10, TimeUnit.SECONDS));
                    return userService.signup(request());
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            int succeeded = 0;
            int rejected = 0;
            for (Future<SignupSuccessResponse<User>> attempt : attempts) {
                try {
                    SignupSuccessResponse<User> response = attempt.get(30, TimeUnit.SECONDS);
                    assertNotNull(response.getMessage());
                    assertNotNull(response.getRefreshToken());
                    succeeded++;
                } catch (ExecutionException exception) {
                    assertInstanceOf(CustomException.class, exception.getCause());
                    rejected++;
                }
            }
            assertEquals(1, succeeded);
            assertEquals(1, rejected);
        } finally {
            executor.shutdownNow();
        }
        transaction.executeWithoutResult(status -> {
            em.clear();
            User accepted = users.findByEmailIgnoreCase(invitedEmail).orElseThrow();
            assertEquals(companyId, accepted.getCompany().getId());
            assertEquals(roleId, accepted.getRole().getId());
            UserInvitation invitation = findInvitation();
            assertNotNull(invitation.getConsumedAt());
            assertEquals(accepted.getId(), invitation.getAcceptedUserId());
            assertEquals(inviterId, invitation.getInvitedById());
            assertEquals(sessionsBefore + 1, refreshTokens.count());
            assertEquals(1, users.findByCompany_Id(companyId).stream()
                    .filter(user -> user.getEmail().equals(invitedEmail)).count());
        });
    }

    @Test
    void sessionFailure_rollsBackAccountConsumptionAndSession_thenInvitationCanBeRetried() {
        long sessionsBefore = refreshTokens.count();
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new IllegalStateException("Simulated failure after refresh token persistence");
        }).when(refreshTokenService).createRefreshToken(any(User.class));

        assertThrows(IllegalStateException.class, () -> userService.signup(request()));
        transaction.executeWithoutResult(status -> {
            em.clear();
            assertFalse(users.existsByEmailIgnoreCase(invitedEmail));
            assertEquals(sessionsBefore, refreshTokens.count());
            UserInvitation invitation = findInvitation();
            assertNull(invitation.getConsumedAt());
            assertNull(invitation.getAcceptedUserId());
        });

        reset(refreshTokenService);
        assertTrue(userService.signup(request()).isSuccess());
        assertTrue(users.existsByEmailIgnoreCase(invitedEmail));
        assertEquals(sessionsBefore + 1, refreshTokens.count());
    }

    @Test
    void renewal_revokesEarlierSecretAndKeepsItsAudit() {
        String original = token;
        transaction.executeWithoutResult(status -> {
            IssuedInvitationDTO renewed = invitationService.issue(invitedEmail, roles.findById(roleId).orElseThrow(),
                    users.findById(inviterId).orElseThrow());
            token = renewed.getInvitationLink().split("#invitationToken=", 2)[1];
            em.flush();
            em.clear();
        });
        assertThrows(CustomException.class, () -> invitationService.preview(original));
        assertEquals(invitedEmail, invitationService.preview(token).getEmail());
        assertTrue(userService.signup(request()).isSuccess());
        transaction.executeWithoutResult(status -> {
            em.clear();
            List<UserInvitation> history = invitations.findAll().stream()
                    .filter(inv -> invitedEmail.equals(inv.getEmail())).toList();
            assertEquals(2, history.size());
            assertEquals(1, history.stream().filter(inv -> inv.getRevokedAt() != null).count());
            assertEquals(1, history.stream().filter(inv -> inv.getConsumedAt() != null).count());
        });
    }

    private UserSignupRequest request() {
        return UserSignupRequest.builder().email(invitedEmail).firstName("Accepted").lastName("User")
                .phone("12345678").password("A-long-test-password-482!").invitationToken(token).build();
    }

    private UserInvitation findInvitation() {
        return invitations.findAll().stream().filter(inv -> invitedEmail.equals(inv.getEmail()))
                .filter(inv -> inv.getRevokedAt() == null).findFirst().orElseThrow();
    }
}
