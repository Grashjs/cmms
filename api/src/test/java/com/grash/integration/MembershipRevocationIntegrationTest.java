package com.grash.integration;

import com.grash.dto.AuthTokens;
import com.grash.dto.IssuedInvitationDTO;
import com.grash.dto.LdapLoginRequest;
import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.enums.RoleCode;
import com.grash.repository.*;
import com.grash.security.JwtTokenProvider;
import com.grash.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.naming.directory.SearchControls;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;

/** Real PostgreSQL, revocation, JWTs and transactions; only the external LDAP server is mocked. */
class MembershipRevocationIntegrationTest extends AbstractIntegrationTest {
    @Autowired LdapService ldapService;
    @Autowired SubscriptionService subscriptionService;
    @Autowired UserInvitationService invitationService;
    @Autowired CompanyAuthorizationService authorization;
    @Autowired RefreshTokenService tokenService;
    @Autowired JwtTokenProvider jwt;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired CompanyRepository companies;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired SubscriptionPlanRepository plans;
    @Autowired CurrencyRepository currencies;
    @Autowired UserInvitationRepository invitations;
    @Autowired RefreshTokenRepository refreshTokens;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean LdapTemplate ldap;
    @MockitoBean LdapAuthenticationProvider ldapAuthenticationProvider;

    TransactionTemplate tx;
    Company company;
    User owner, issuer;
    Role admin;
    AuthTokens oldTokens;
    String pendingToken, pendingEmail;

    @BeforeEach void fixture() {
        tx = new TransactionTemplate(transactions);
        tx.executeWithoutResult(status -> {
            company = new Company(); company.setName("Membership revocation"); company.setInvitedUsers(true);
            Subscription subscription = new Subscription(); subscription.setUsersCount(100);
            subscription.setSubscriptionPlan(plans.findByCode("BUSINESS").orElseThrow());
            company.setSubscription(subscriptions.save(subscription));
            company.getCompanySettings().getGeneralPreferences().setCurrency(currencies.findFirstBy().orElseThrow());
            company.getCompanySettings().getGeneralPreferences().setTimeZone("UTC");
            companies.saveAndFlush(company);
            admin = roles.findDefaultRoleWithCode(RoleCode.ADMIN).orElseThrow();
            owner = user(true); issuer = user(false);
            issuer.setCreatedViaSso(true); issuer.setSsoProvider("LDAP"); issuer.setSsoProviderId("ldap-member");
            users.saveAndFlush(issuer);
            oldTokens = tokenService.createTokenPair(issuer);
            em.flush(); em.clear();
        });
        pendingEmail = "membership-invite-" + UUID.randomUUID() + "@example.test";
        pendingToken = token(invitationService.issue(pendingEmail, admin, issuer));
        assertDoesNotThrow(() -> invitationService.preview(pendingToken));
        ReflectionTestUtils.setField(ldapService, "ldapOrgAdmin", owner.getEmail());
        ReflectionTestUtils.setField(ldapService, "usernameAttr", "uid");
        ReflectionTestUtils.setField(ldapService, "objectClassAttr", "inetOrgPerson");
        ReflectionTestUtils.setField(ldapService, "ldapOuRoleMappings", "administrators=ADMIN|technicians=LIMITED_TECHNICIAN");
        ReflectionTestUtils.setField(ldapService, "ldapSyncEnabled", true);
        ReflectionTestUtils.setField(ldapService, "ldapSyncDisable", true);
        ReflectionTestUtils.setField(ldapService, "ldapSyncUpdate", false);
    }

    private User user(boolean ownsCompany) {
        User user = new User(); user.setCompany(company); user.setRole(admin); user.setOwnsCompany(ownsCompany);
        user.setFirstName("Membership"); user.setLastName("Fixture"); user.setEnabled(true);
        user.setEmail("membership-" + UUID.randomUUID() + "@example.test");
        user.setUsername(UUID.randomUUID().toString()); user.setPassword("fixture-password-hash");
        return users.saveAndFlush(user);
    }

    @AfterEach void cleanup() {
        tx.executeWithoutResult(status -> {
            if (company != null) companies.findById(company.getId()).ifPresent(companies::delete);
            em.flush(); em.clear();
        });
    }

    private String token(IssuedInvitationDTO invitation) {
        return invitation.getInvitationLink().split("#invitationToken=", 2)[1];
    }

    private AuthTokens signinWithOu(String ou) {
        doReturn(List.of("uid=ldap-member,ou=" + ou)).when(ldap).search(anyString(), anyString(), any(ContextMapper.class));
        return ldapService.signinLdap(new LdapLoginRequest("ldap-member", "harmless-test-password"));
    }

    private void syncMembers(List<String> members) {
        doReturn(members).when(ldap).search(anyString(), anyString(), any(SearchControls.class), any(AttributesMapper.class));
        ldapService.syncLdapUsers();
    }

    private void assertPermanentlyRevoked() {
        assertThrows(CustomException.class, () -> invitationService.preview(pendingToken));
        assertThrows(CustomException.class, () -> jwt.getAuthentication(oldTokens.getAccessToken()));
        assertThrows(CustomException.class, () -> tokenService.rotate(oldTokens.getRefreshToken()));
        tx.executeWithoutResult(status -> {
            em.clear();
            assertNotNull(users.findById(issuer.getId()).orElseThrow().getSessionInvalidatedAt());
            assertNotNull(invitations.findAll().stream().filter(i -> pendingEmail.equals(i.getEmail()))
                    .findFirst().orElseThrow().getRevokedAt());
        });
    }

    private void assertOriginalGrantsStillValid() {
        assertDoesNotThrow(() -> invitationService.preview(pendingToken));
        assertDoesNotThrow(() -> jwt.getAuthentication(oldTokens.getAccessToken()));
        tx.executeWithoutResult(status -> {
            em.clear();
            assertNull(users.findById(issuer.getId()).orElseThrow().getSessionInvalidatedAt());
            assertTrue(refreshTokens.findAll().stream().filter(t -> t.getUser().getId().equals(issuer.getId()))
                    .noneMatch(RefreshToken::isRevoked));
        });
    }

    @Test void ldapRoleWithdrawalThenRestorationDoesNotReviveOldInvitationsOrSessions() {
        signinWithOu("technicians");
        AuthTokens restored = signinWithOu("administrators");
        assertPermanentlyRevoked();
        assertDoesNotThrow(() -> jwt.getAuthentication(restored.getAccessToken()));
        assertDoesNotThrow(() -> tokenService.rotate(restored.getRefreshToken()));
    }

    @Test void ldapDisableThenReenableDoesNotReviveOldInvitationsOrSessions() {
        syncMembers(List.of());
        syncMembers(List.of("ldap-member"));
        assertPermanentlyRevoked();
        tx.executeWithoutResult(status -> assertTrue(users.findById(issuer.getId()).orElseThrow().isEnabled()));
    }

    @Test void unchangedLdapRoleAndMembershipKeepExistingGrants() {
        signinWithOu("administrators");
        syncMembers(List.of("ldap-member"));
        assertOriginalGrantsStillValid();
    }

    @Test void subscriptionDowngradeThenUpgradeDoesNotReviveOldInvitationsOrSessions() {
        subscriptionService.downgrade(List.of(issuer.getId()), owner);
        subscriptionService.upgrade(List.of(issuer.getId()), owner);
        assertPermanentlyRevoked();
        tx.executeWithoutResult(status -> {
            User restored = users.findById(issuer.getId()).orElseThrow();
            assertTrue(restored.isEnabled()); assertTrue(restored.isEnabledInSubscription());
        });
    }

    @Test void emptySubscriptionChangesLeaveExistingGrantsUntouched() {
        subscriptionService.downgrade(List.of(), owner);
        subscriptionService.upgrade(List.of(), owner);
        assertOriginalGrantsStillValid();
    }

    @Test void rolledBackLdapRoleChangeDoesNotCommitPartialRevocation() {
        tx.executeWithoutResult(status -> {
            signinWithOu("technicians");
            status.setRollbackOnly();
        });
        assertOriginalGrantsStillValid();
        tx.executeWithoutResult(status -> assertEquals(admin.getId(), users.findById(issuer.getId()).orElseThrow().getRole().getId()));
    }

    @Test void rolledBackSubscriptionChangeDoesNotCommitPartialRevocation() {
        tx.executeWithoutResult(status -> {
            subscriptionService.downgrade(List.of(issuer.getId()), owner);
            status.setRollbackOnly();
        });
        assertOriginalGrantsStillValid();
        tx.executeWithoutResult(status -> assertTrue(users.findById(issuer.getId()).orElseThrow().isEnabled()));
    }

    @Test void subscriptionChangeUsesTheSameCompanyLockAsInvitationAcceptance() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch locked = new CountDownLatch(1), release = new CountDownLatch(1), started = new CountDownLatch(1);
        try {
            Future<?> holder = executor.submit(() -> tx.executeWithoutResult(status -> {
                authorization.lockCompany(company.getId()); locked.countDown();
                try { assertTrue(release.await(10, TimeUnit.SECONDS)); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
            }));
            assertTrue(locked.await(5, TimeUnit.SECONDS));
            Future<?> mutation = executor.submit(() -> {
                started.countDown(); subscriptionService.downgrade(List.of(issuer.getId()), owner);
            });
            assertTrue(started.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> mutation.get(200, TimeUnit.MILLISECONDS));
            release.countDown(); holder.get(10, TimeUnit.SECONDS); mutation.get(10, TimeUnit.SECONDS);
            assertPermanentlyRevoked();
        } finally { release.countDown(); executor.shutdownNow(); }
    }
}
