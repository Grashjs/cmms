package com.grash.integration;

import com.grash.dto.*;
import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.enums.*;
import com.grash.repository.*;
import com.grash.security.JwtTokenProvider;
import com.grash.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real PostgreSQL transactions, authorization reads, JWTs, caches and HTTP service wiring. */
@AutoConfigureMockMvc
// Match application.yml when serializing the Role Hibernate proxy in real HTTP responses.
@org.springframework.test.context.TestPropertySource(properties = "spring.jackson.serialization.fail-on-empty-beans=false")
class CompanyAuthorizationIntegrationTest extends AbstractIntegrationTest {
    @Autowired UserService userService;
    @Autowired UserInvitationService invitationService;
    @Autowired RoleService roleService;
    @Autowired CompanyAuthorizationService authorization;
    @Autowired RefreshTokenService tokenService;
    @Autowired CacheService cache;
    @Autowired JwtTokenProvider jwt;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired CompanyRepository companies;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired SubscriptionPlanRepository plans;
    @Autowired CurrencyRepository currencies;
    @Autowired UserInvitationRepository invitations;
    @Autowired RefreshTokenRepository refreshTokens;
    @Autowired PlatformTransactionManager transactions;
    @Autowired EntityManager em;
    @Autowired MockMvc http;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder passwords;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    @Autowired VerificationTokenRepository verificationTokens;
    @Autowired javax.sql.DataSource dataSource;

    TransactionTemplate tx;
    Company company, otherCompany;
    User owner, admin, manager, subordinate, foreign;
    Role low, managerRole, foreignRole;

    @BeforeEach void fixture() {
        tx = new TransactionTemplate(transactions);
        tx.executeWithoutResult(status -> {
            company = company("Authorization"); otherCompany = company("Other tenant");
            Role adminRole = roles.findDefaultRoleWithCode(RoleCode.ADMIN).orElseThrow();
            low = roles.save(Role.builder().name("Low").roleType(RoleType.ROLE_CLIENT).code(RoleCode.USER_CREATED)
                    .companySettings(company.getCompanySettings()).createPermissions(new HashSet<>(Set.of(PermissionEntity.PEOPLE_AND_TEAMS))).build());
            managerRole = roles.save(Role.builder().name("People manager").roleType(RoleType.ROLE_CLIENT).code(RoleCode.USER_CREATED)
                    .companySettings(company.getCompanySettings()).createPermissions(new HashSet<>(Set.of(PermissionEntity.PEOPLE_AND_TEAMS, PermissionEntity.SETTINGS)))
                    .viewPermissions(new HashSet<>(Set.of(PermissionEntity.SETTINGS)))
                    .editOtherPermissions(new HashSet<>(Set.of(PermissionEntity.PEOPLE_AND_TEAMS, PermissionEntity.SETTINGS))).build());
            foreignRole = roles.save(Role.builder().name("Foreign role").roleType(RoleType.ROLE_CLIENT).code(RoleCode.USER_CREATED)
                    .companySettings(otherCompany.getCompanySettings()).build());
            owner = user(company, adminRole); owner.setOwnsCompany(true); users.save(owner);
            admin = user(company, adminRole); manager = user(company, managerRole);
            subordinate = user(company, low); foreign = user(otherCompany, foreignRole);
            em.flush(); em.clear();
        });
    }

    Company company(String name) {
        Company result = new Company(); result.setName(name); result.setInvitedUsers(true);
        Subscription subscription = new Subscription(); subscription.setUsersCount(100);
        subscription.setSubscriptionPlan(plans.findByCode("BUSINESS").orElseThrow());
        result.setSubscription(subscriptions.save(subscription));
        result.getCompanySettings().getGeneralPreferences().setCurrency(currencies.findFirstBy().orElseThrow());
        result.getCompanySettings().getGeneralPreferences().setTimeZone("UTC");
        return companies.saveAndFlush(result);
    }

    User user(Company tenant, Role role) {
        User result = new User(); result.setCompany(tenant); result.setRole(role);
        result.setFirstName("Test"); result.setLastName("Authorization"); result.setEnabled(true);
        result.setEmail("authorization-" + UUID.randomUUID() + "@example.com");
        result.setUsername(UUID.randomUUID().toString()); result.setPassword("unchanged-fixture-hash");
        return users.saveAndFlush(result);
    }

    @AfterEach void cleanup() {
        ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", false);
        tx.executeWithoutResult(status -> {
            if (company != null) companies.findById(company.getId()).ifPresent(companies::delete);
            if (otherCompany != null) companies.findById(otherCompany.getId()).ifPresent(companies::delete);
            em.flush(); em.clear();
        });
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void deniedRequestsLeaveUsersInvitationsPasswordsAndSessionsUntouched(boolean email) {
        ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", email);
        long usersBefore = users.count(), invitationsBefore = invitations.count(), sessionsBefore = refreshTokens.count();
        UserInvitationDTO invite = new UserInvitationDTO(); invite.setRole(admin.getRole());
        invite.setEmails(List.of("forbidden-" + UUID.randomUUID() + "@example.com")); invite.setDisableSendingEmail(true);
        assertThrows(CustomException.class, () -> userService.inviteUsers(invite, manager));
        assertThrows(CustomException.class, () -> invitationService.issue(invite.getEmails().iterator().next(), admin.getRole(), manager));
        assertThrows(CustomException.class, () -> userService.patchUserRole(subordinate.getId(), admin.getRole().getId(), manager));
        assertThrows(CustomException.class, () -> userService.patchUserRole(admin.getId(), low.getId(), manager));
        assertThrows(CustomException.class, () -> userService.patchUserRole(owner.getId(), low.getId(), admin));
        assertThrows(CustomException.class, () -> userService.patchUserRole(foreign.getId(), low.getId(), admin));
        assertThrows(CustomException.class, () -> userService.patchUserRole(subordinate.getId(), foreignRole.getId(), admin));
        Role platform = roles.findByCodeAndRoleType(RoleCode.ADMIN, RoleType.ROLE_SUPER_ADMIN).get(0);
        assertThrows(CustomException.class, () -> userService.patchUserRole(subordinate.getId(), platform.getId(), owner));
        UserPatchDTO password = new UserPatchDTO(); password.setNewPassword("Harmless-new-password-42!");
        assertThrows(CustomException.class, () -> userService.update(admin.getId(), password, manager));
        assertThrows(CustomException.class, () -> userService.update(subordinate.getId(), password, manager));
        assertThrows(CustomException.class, () -> userService.update(owner.getId(), password, admin));
        assertThrows(CustomException.class, () -> userService.softDeleteUser(subordinate.getId(), manager));
        assertThrows(CustomException.class, () -> userService.softDeleteUser(owner.getId(), admin));
        RolePatchDTO rolePatch = new RolePatchDTO(); rolePatch.setViewPermissions(List.of(PermissionEntity.ANALYTICS));
        assertThrows(CustomException.class, () -> roleService.patch(low.getId(), rolePatch, manager));
        assertThrows(CustomException.class, () -> roleService.patch(managerRole.getId(), new RolePatchDTO(), manager));
        tx.executeWithoutResult(status -> {
            em.clear();
            assertEquals("unchanged-fixture-hash", users.findById(admin.getId()).orElseThrow().getPassword());
            assertEquals(low.getId(), users.findById(subordinate.getId()).orElseThrow().getRole().getId());
            assertEquals(subordinate.getEmail(), users.findById(subordinate.getId()).orElseThrow().getEmail());
            assertTrue(users.findById(subordinate.getId()).orElseThrow().isEnabled());
            assertTrue(roles.findById(low.getId()).orElseThrow().getViewPermissions().isEmpty());
        });
        assertEquals(usersBefore, users.count()); assertEquals(invitationsBefore, invitations.count());
        assertEquals(sessionsBefore, refreshTokens.count());
    }

    @Test void roleEditsRevokeSessionsCachesAndInvitationsPermanently() {
        User[] stalePrincipal = new User[1];
        AuthTokens old = tx.execute(status -> {
            User current = users.findById(subordinate.getId()).orElseThrow(); cache.putUserInCache(current);
            stalePrincipal[0] = current;
            return tokenService.createTokenPair(current);
        });
        String email = "pending-" + UUID.randomUUID() + "@example.com";
        String token = token(invitationService.issue(email, low, subordinate));
        RolePatchDTO patch = new RolePatchDTO(); patch.setCreatePermissions(List.of());
        roleService.patch(low.getId(), patch, owner);
        assertTrue(cache.getUserFromCache(subordinate.getEmail()).isEmpty());
        // Simulate a stale cache on another API instance: authentication still reads the database.
        cache.putUserInCache(stalePrincipal[0]);
        assertThrows(CustomException.class, () -> jwt.getAuthentication(old.getAccessToken()));
        assertThrows(CustomException.class, () -> tokenService.rotate(old.getRefreshToken()));
        assertThrows(CustomException.class, () -> invitationService.preview(token));
        patch.setCreatePermissions(List.of(PermissionEntity.PEOPLE_AND_TEAMS));
        roleService.patch(low.getId(), patch, owner);
        assertThrows(CustomException.class, () -> userService.signup(signup(email, token)));
        assertFalse(users.existsByEmailIgnoreCase(email));
        tx.executeWithoutResult(status -> {
            em.clear();
            assertNotNull(invitations.findAll().stream().filter(i -> email.equals(i.getEmail())).findFirst().orElseThrow().getRevokedAt());
        });
    }

    @Test void changingInvitedRoleRequiresRenewalEvenWhenIssuerRemainsAdministrator() {
        String email = "changed-grant-" + UUID.randomUUID() + "@example.com";
        String original = token(invitationService.issue(email, low, owner));
        RolePatchDTO patch = new RolePatchDTO(); patch.setViewPermissions(List.of(PermissionEntity.FILES));
        roleService.patch(low.getId(), patch, owner);
        long sessionsBefore = refreshTokens.count();
        assertThrows(CustomException.class, () -> userService.signup(signup(email, original)));
        assertFalse(users.existsByEmailIgnoreCase(email)); assertEquals(sessionsBefore, refreshTokens.count());
        String renewed = token(invitationService.issue(email, low, owner));
        assertTrue(userService.signup(signup(email, renewed)).isSuccess());
    }

    @Test void invitationWithoutIssuanceSnapshotsFailsClosedAfterUpgrade() {
        String email = "old-grant-" + UUID.randomUUID() + "@example.com";
        String original = token(invitationService.issue(email, low, owner));
        tx.executeWithoutResult(status -> {
            em.createNativeQuery("UPDATE user_invitation SET role_authorization_hash=NULL, issuer_authorization_hash=NULL WHERE email=:email")
                    .setParameter("email", email).executeUpdate();
            em.clear();
        });
        long sessionsBefore = refreshTokens.count();
        assertThrows(CustomException.class, () -> userService.signup(signup(email, original)));
        assertFalse(users.existsByEmailIgnoreCase(email)); assertEquals(sessionsBefore, refreshTokens.count());
        assertTrue(userService.signup(signup(email, token(invitationService.issue(email, low, owner)))).isSuccess());
    }

    @Test void issuerDemotionSerializesWithConcurrentAcceptance() throws Exception {
        String email = "concurrent-" + UUID.randomUUID() + "@example.com";
        String token = token(invitationService.issue(email, low, manager));
        long sessionsBefore = refreshTokens.count();
        CountDownLatch changed = new CountDownLatch(1), commit = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> demotion = pool.submit(() -> tx.executeWithoutResult(status -> {
                // low cannot grant manager's earlier snapshot; all issuer links are revoked on the role change.
                userService.patchUserRole(manager.getId(), low.getId(), owner);
                changed.countDown();
                try { assertTrue(commit.await(10, TimeUnit.SECONDS)); }
                catch (InterruptedException ex) { throw new RuntimeException(ex); }
            }));
            assertTrue(changed.await(10, TimeUnit.SECONDS));
            Future<?> acceptance = pool.submit(() -> userService.signup(signup(email, token)));
            assertThrows(TimeoutException.class, () -> acceptance.get(200, TimeUnit.MILLISECONDS));
            commit.countDown(); demotion.get(10, TimeUnit.SECONDS);
            ExecutionException rejected = assertThrows(ExecutionException.class, () -> acceptance.get(10, TimeUnit.SECONDS));
            assertInstanceOf(CustomException.class, rejected.getCause());
        } finally { commit.countDown(); pool.shutdownNow(); }
        assertFalse(users.existsByEmailIgnoreCase(email)); assertEquals(sessionsBefore, refreshTokens.count());
        assertThrows(CustomException.class, () -> invitationService.issue("stale-issuer@example.com", managerRole, manager));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void allowedInvitationRegistrationAndRoleChangeStillWork(boolean emailDelivery) {
        ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", emailDelivery);
        String email = "allowed-" + UUID.randomUUID() + "@example.com";
        UserInvitationDTO request = new UserInvitationDTO(); request.setRole(low); request.setEmails(List.of(email));
        request.setDisableSendingEmail(!emailDelivery);
        InvitationResponse issued = userService.inviteUsers(request, manager);
        assertTrue(userService.signup(signup(email, token(issued.getInvitations().get(0)))).isSuccess());
        User changed = userService.patchUserRole(subordinate.getId(), managerRole.getId(), owner);
        assertEquals(managerRole.getId(), changed.getRole().getId());
    }

    @ParameterizedTest @ValueSource(strings = {"self", "admin-self", "admin-other"})
    void profilePasswordHttpIsForbiddenWithEmailWithoutChangingAccountsOrSessions(String mode) throws Exception {
        ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", true);
        User actor = mode.equals("self") ? subordinate : admin;
        User subject = mode.equals("admin-other") ? subordinate : actor;
        AuthTokens actorTokens = session(actor);
        AuthTokens subjectTokens = subject == actor ? actorTokens : session(subject);
        AccountState before = accountState(subject);
        var sessionsBefore = sessionState();

        http.perform(patch("/users/" + subject.getId()).header("Authorization", "Bearer " + actorTokens.getAccessToken())
                        .contentType("application/json")
                        .content("{\"firstName\":\"Must not change\",\"newPassword\":\"Blocked-password-42!\"}"))
                .andExpect(status().isForbidden());

        assertEquals(before, accountState(subject));
        assertEquals(sessionsBefore, sessionState());
        assertTrue(jwt.getAuthentication(actorTokens.getAccessToken()).isAuthenticated());
        assertTrue(jwt.getAuthentication(subjectTokens.getAccessToken()).isAuthenticated());
    }

    @ParameterizedTest @ValueSource(strings = {"self", "admin-self", "admin-other"})
    void profilePasswordHttpWithoutEmailPreservesAuthorizedChangesAndRevokesSessions(String mode) throws Exception {
        ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", false);
        User actor = mode.equals("self") ? subordinate : admin;
        User subject = mode.equals("admin-other") ? subordinate : actor;
        AuthTokens actorTokens = session(actor);
        AuthTokens subjectTokens = subject == actor ? actorTokens : session(subject);
        long tokenCount = refreshTokens.count();

        // Historical profile API: deliberately no oldPassword on this path in email=false mode.
        http.perform(patch("/users/" + subject.getId()).header("Authorization", "Bearer " + actorTokens.getAccessToken())
                        .contentType("application/json").content("{\"newPassword\":\"Allowed-password-42!\"}"))
                .andExpect(status().isOk());

        assertTrue(passwords.matches("Allowed-password-42!", accountState(subject).password()));
        assertNotNull(accountState(subject).invalidatedAt());
        assertEquals(tokenCount, refreshTokens.count()); // profile editing never issues a replacement session
        assertSessionsRevoked(subject, subjectTokens);
        if (subject != actor) assertTrue(jwt.getAuthentication(actorTokens.getAccessToken()).isAuthenticated());
    }

    @ParameterizedTest @ValueSource(strings = {"limited", "creator", "owner", "foreign", "superior"})
    void profilePasswordHttpWithoutEmailStillEnforcesTheCompanyPolicy(String scenario) throws Exception {
        ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", false);
        User actor = switch (scenario) {
            case "owner", "foreign" -> admin;
            case "creator" -> subordinate;
            default -> manager;
        };
        User subject = switch (scenario) {
            case "owner" -> owner;
            case "foreign" -> foreign;
            case "superior" -> admin;
            case "creator" -> manager;
            default -> subordinate;
        };
        if (scenario.equals("creator")) tx.executeWithoutResult(status -> {
            // createdBy is immutable through JPA after insertion; establish historical test data explicitly.
            em.createNativeQuery("update own_user set created_by = :creator where id = :target")
                    .setParameter("creator", actor.getId()).setParameter("target", subject.getId()).executeUpdate();
            em.clear();
            assertTrue(users.findById(subject.getId()).orElseThrow().canBeEditedBy(users.findById(actor.getId()).orElseThrow()));
        });
        AuthTokens actorTokens = session(actor), subjectTokens = session(subject);
        AccountState before = accountState(subject);
        var sessionsBefore = sessionState();

        http.perform(patch("/users/" + subject.getId()).header("Authorization", "Bearer " + actorTokens.getAccessToken())
                        .contentType("application/json").content("{\"newPassword\":\"Forbidden-password-42!\"}"))
                .andExpect(status().is(scenario.equals("foreign") ? 404 : 403));

        assertEquals(before, accountState(subject));
        assertEquals(sessionsBefore, sessionState());
        assertTrue(jwt.getAuthentication(actorTokens.getAccessToken()).isAuthenticated());
        assertTrue(jwt.getAuthentication(subjectTokens.getAccessToken()).isAuthenticated());
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void ordinaryProfileHttpEditsWorkInBothModesWithoutRevokingSessions(boolean email) throws Exception {
        ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", email);
        AuthTokens personal = session(subordinate), administrator = session(admin);
        AccountState before = accountState(subordinate);
        var sessionsBefore = sessionState();
        for (AuthTokens actorTokens : List.of(personal, administrator)) {
            http.perform(patch("/users/" + subordinate.getId()).header("Authorization", "Bearer " + actorTokens.getAccessToken())
                            .contentType("application/json").content("{\"firstName\":\"Updated profile\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.firstName").value("Updated profile"));
        }
        AccountState after = accountState(subordinate);
        assertEquals("Updated profile", after.firstName());
        assertEquals(before.password(), after.password());
        assertEquals(before.invalidatedAt(), after.invalidatedAt());
        assertEquals(sessionsBefore, sessionState());
        assertTrue(jwt.getAuthentication(personal.getAccessToken()).isAuthenticated());
        assertTrue(jwt.getAuthentication(administrator.getAccessToken()).isAuthenticated());
    }

    @Test void directHttpCannotBypassPolicyAndAssignableRolesAreFiltered() throws Exception {
        String bearer = tx.execute(status -> tokenService.createTokenPair(users.findById(manager.getId()).orElseThrow()).getAccessToken());
        http.perform(get("/roles/assignable").header("Authorization", "Bearer " + bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.code == 'ADMIN')]").isEmpty())
                .andExpect(jsonPath("$[?(@.id == " + low.getId() + ")]").isNotEmpty());
        http.perform(patch("/users/" + admin.getId()).header("Authorization", "Bearer " + bearer)
                        .contentType("application/json").content("{\"newPassword\":\"Harmless-password-42!\"}"))
                .andExpect(status().isForbidden());
        http.perform(patch("/users/" + subordinate.getId() + "/role?role=" + admin.getRole().getId())
                        .header("Authorization", "Bearer " + bearer)).andExpect(status().isForbidden());
        http.perform(post("/users/invite").header("Authorization", "Bearer " + bearer)
                        .contentType("application/json").content("{\"emails\":[\"forbidden-http@example.com\"],\"role\":{\"id\":" + admin.getRole().getId() + "}}"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void ownPasswordChangeReturnsAnImmediatelyUsableSession(boolean email) throws Exception {
        ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", email);
        String bearer = tx.execute(status -> {
            User current = users.findById(owner.getId()).orElseThrow();
            current.setPassword(passwords.encode("Old-legitimate-password-42!")); users.saveAndFlush(current);
            return tokenService.createTokenPair(current).getAccessToken();
        });
        AccountState before = accountState(owner);
        var sessionsBefore = sessionState();
        http.perform(post("/auth/updatepwd").header("Authorization", "Bearer " + bearer)
                        .contentType("application/json")
                        .content("{\"oldPassword\":\"Incorrect-old-password\",\"newPassword\":\"New-legitimate-password-42!\"}"))
                .andExpect(status().isNotAcceptable());
        assertEquals(before, accountState(owner));
        assertEquals(sessionsBefore, sessionState());
        String response = http.perform(post("/auth/updatepwd").header("Authorization", "Bearer " + bearer)
                        .contentType("application/json")
                        .content("{\"oldPassword\":\"Old-legitimate-password-42!\",\"newPassword\":\"New-legitimate-password-42!\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String fresh = json.readTree(response).get("accessToken").asText();
        assertTrue(jwt.getAuthentication(fresh).isAuthenticated());
        assertThrows(CustomException.class, () -> jwt.getAuthentication(bearer));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void verifiedAccountRecoveryStillWorksForOwnerAndConsumesItsToken(boolean email) throws Exception {
        ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", email);
        AuthTokens previousSession = session(owner);
        String recovery = UUID.randomUUID().toString();
        tx.executeWithoutResult(status -> verificationTokens.saveAndFlush(new VerificationToken(recovery,
                users.findById(owner.getId()).orElseThrow(), "Recovered-password-42!")));
        http.perform(get("/auth/reset-pwd-confirm").param("token", recovery))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/account/login?email=")));
        tx.executeWithoutResult(status -> {
            em.clear();
            assertTrue(passwords.matches("Recovered-password-42!", users.findById(owner.getId()).orElseThrow().getPassword()));
            assertNull(verificationTokens.findVerificationTokenEntityByToken(recovery));
        });
        assertSessionsRevoked(owner, previousSession);
        AccountState recovered = accountState(owner);
        var sessionsAfterRecovery = sessionState();
        http.perform(get("/auth/reset-pwd-confirm").param("token", recovery))
                .andExpect(status().isFound()).andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/account/register")));
        assertEquals(recovered, accountState(owner));
        assertEquals(sessionsAfterRecovery, sessionState());
    }

    record AccountState(String password, Date invalidatedAt, String firstName) {}
    record SessionState(boolean revoked, String replacedBy, Date createdAt, Date expiresAt) {}

    AccountState accountState(User user) {
        return tx.execute(status -> {
            em.clear(); User current = users.findById(user.getId()).orElseThrow();
            return new AccountState(current.getPassword(), current.getSessionInvalidatedAt(), current.getFirstName());
        });
    }

    Map<Long, SessionState> sessionState() {
        return tx.execute(status -> {
            em.clear();
            return refreshTokens.findAll().stream().collect(java.util.stream.Collectors.toMap(RefreshToken::getId,
                    token -> new SessionState(token.isRevoked(), token.getReplacedByTokenHash(), token.getCreatedAt(), token.getExpiresAt())));
        });
    }

    AuthTokens session(User user) {
        return tx.execute(status -> tokenService.createTokenPair(users.findById(user.getId()).orElseThrow()));
    }

    void assertSessionsRevoked(User user, AuthTokens previous) {
        assertThrows(CustomException.class, () -> jwt.getAuthentication(previous.getAccessToken()));
        tx.executeWithoutResult(status -> {
            var tokens = refreshTokens.findAll().stream().filter(token -> token.getUser().getId().equals(user.getId())).toList();
            assertFalse(tokens.isEmpty()); assertTrue(tokens.stream().allMatch(RefreshToken::isRevoked));
        });
    }

    @Test void deploymentAuditQueriesExecuteReadOnlyAgainstTheMigratedSchema() throws Exception {
        long accountsBefore = users.count(), invitationsBefore = invitations.count();
        String sql = java.nio.file.Files.readString(java.nio.file.Path.of("../scripts/audit-company-authorization.sql"));
        try (java.sql.Connection connection = dataSource.getConnection(); java.sql.Statement statement = connection.createStatement()) {
            boolean rows = statement.execute(sql);
            while (rows || statement.getUpdateCount() != -1) {
                if (rows) try (java.sql.ResultSet results = statement.getResultSet()) { while (results.next()) { /* no personal data or secrets in logs */ } }
                rows = statement.getMoreResults();
            }
        }
        assertEquals(accountsBefore, users.count()); assertEquals(invitationsBefore, invitations.count());
    }

    String token(IssuedInvitationDTO invitation) { return invitation.getInvitationLink().split("#invitationToken=", 2)[1]; }
    UserSignupRequest signup(String email, String token) {
        return UserSignupRequest.builder().email(email).firstName("Accepted").lastName("User").phone("12345678")
                .password("Harmless-signup-password-42!").invitationToken(token).build();
    }
}
