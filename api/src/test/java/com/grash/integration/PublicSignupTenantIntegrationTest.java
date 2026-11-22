package com.grash.integration;

import com.grash.dto.SignupSuccessResponse;
import com.grash.dto.UserSignupRequest;
import com.grash.dto.license.LicensingState;
import com.grash.exception.CustomException;
import com.grash.model.Company;
import com.grash.model.User;
import com.grash.model.enums.RoleCode;
import com.grash.repository.*;
import com.grash.security.CustomUserDetail;
import com.grash.security.JwtTokenProvider;
import com.grash.service.LicenseService;
import com.grash.service.UserInvitationService;
import com.grash.service.UserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercises the real CompanyAudit persistence callback with an authenticated signup request. */
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.jackson.serialization.fail-on-empty-beans=false")
class PublicSignupTenantIntegrationTest extends AbstractIntegrationTest {
    @Autowired UserService service;
    @Autowired UserInvitationService invitations;
    @Autowired UserRepository users;
    @Autowired CompanyRepository companies;
    @Autowired RoleRepository roles;
    @Autowired CurrencyRepository currencies;
    @Autowired RefreshTokenRepository sessions;
    @Autowired VerificationTokenRepository verificationTokens;
    @Autowired JwtTokenProvider jwt;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @Autowired MockMvc http;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    @MockitoBean(answers = org.mockito.Answers.RETURNS_DEEP_STUBS) LicenseService license;

    TransactionTemplate tx;
    User member;
    Long originalCompany;
    String companyName;
    UserSignupRequest request;

    @BeforeEach void prepare() {
        tx = new TransactionTemplate(transactions);
        when(license.hasEntitlement(any())).thenReturn(true);
        when(license.getLicensingState()).thenReturn(LicensingState.builder().hasLicense(true).usersCount(1000).build());
        ReflectionTestUtils.setField(service, "allowedOrganizationAdmins", new String[]{});
        ReflectionTestUtils.setField(service, "PUBLIC_API_URL", "https://signup.example.test");
        ReflectionTestUtils.setField(service, "enableMails", true);
        companyName = "Public signup " + UUID.randomUUID();
        request = UserSignupRequest.builder().email(UUID.randomUUID() + "@example.com")
                .password("Harmless-signup-password-42!").firstName("New").lastName("Owner")
                .phone("12345678").companyName(companyName).timeZone("UTC").build();
        tx.executeWithoutResult(status -> {
            Company company = new Company(); company.setName(companyName + " original");
            company.getCompanySettings().getGeneralPreferences().setCurrency(currencies.findFirstBy().orElseThrow());
            company.getCompanySettings().getGeneralPreferences().setTimeZone("UTC");
            originalCompany = companies.saveAndFlush(company).getId();
            User user = new User(); user.setCompany(company);
            user.setRole(roles.findDefaultRoleWithCode(RoleCode.VIEW_ONLY).orElseThrow());
            user.setEnabled(true); user.setEmail(UUID.randomUUID() + "@example.com");
            user.setUsername(UUID.randomUUID().toString()); user.setPassword("fixture-password-hash");
            user.setFirstName("Limited"); user.setLastName("Member");
            member = users.saveAndFlush(user);
            em.clear();
        });
    }

    @AfterEach void cleanup() {
        SecurityContextHolder.clearContext();
        ReflectionTestUtils.setField(service, "enableInvitationViaEmail", false);
        ReflectionTestUtils.setField(service, "enableMails", false);
        ReflectionTestUtils.setField(service, "PUBLIC_API_URL", "http://localhost:8080");
        ReflectionTestUtils.setField(service, "allowedOrganizationAdmins", new String[]{"admin@test.com"});
        tx.executeWithoutResult(status -> {
            companies.findAll().stream().filter(c -> c.getName() != null && c.getName().startsWith(companyName))
                    .forEach(companies::delete);
            em.flush(); em.clear();
        });
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void authenticatedHttpSignupCreatesAnIndependentCompany(boolean email) throws Exception {
        ReflectionTestUtils.setField(service, "enableInvitationViaEmail", email);
        String bearer = jwt.createToken(member.getEmail(), java.util.List.of(member.getRole().getRoleType()));
        long sessionsBefore = sessions.count();
        http.perform(post("/auth/signup").header("Authorization", "Bearer " + bearer)
                .contentType("application/json").content(json.writeValueAsString(request)))
                .andExpect(status().isOk());
        assertIndependentCompany(email);
        assertEquals(sessionsBefore + (email ? 0 : 1), sessions.count());
        if (email) assertEquals(Boolean.TRUE, tx.execute(status -> verificationTokens.findAll().stream()
                .anyMatch(token -> token.getUser().getEmail().equals(request.getEmail()))));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void directServiceSignupPreservesCallerContextAndLegitimateCreation(boolean email) {
        ReflectionTestUtils.setField(service, "enableInvitationViaEmail", email);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                CustomUserDetail.builder().user(member).build(), null, java.util.List.of()));
        SecurityContextHolder.setContext(context);
        SignupSuccessResponse<User> response = service.signup(request);
        assertTrue(response.isSuccess());
        assertSame(context, SecurityContextHolder.getContext());
        SecurityContextHolder.clearContext();
        assertIndependentCompany(email);
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void authenticatedAcceptanceUsesTheInvitationCompanyAndRole(boolean email) throws Exception {
        ReflectionTestUtils.setField(service, "enableInvitationViaEmail", email);
        var issued = tx.execute(status -> {
            Company company = new Company(); company.setName(companyName + " invited");
            company.getCompanySettings().getGeneralPreferences().setCurrency(currencies.findFirstBy().orElseThrow());
            company.getCompanySettings().getGeneralPreferences().setTimeZone("UTC");
            companies.saveAndFlush(company);
            User issuer = new User(); issuer.setCompany(company);
            issuer.setRole(roles.findDefaultRoleWithCode(RoleCode.ADMIN).orElseThrow());
            issuer.setEnabled(true); issuer.setOwnsCompany(true);
            issuer.setEmail(UUID.randomUUID() + "@example.com");
            issuer.setUsername(UUID.randomUUID().toString()); issuer.setPassword("fixture-password-hash");
            issuer.setFirstName("Invitation"); issuer.setLastName("Issuer");
            users.saveAndFlush(issuer);
            return invitations.issue(request.getEmail(),
                    roles.findDefaultRoleWithCode(RoleCode.VIEW_ONLY).orElseThrow(), issuer);
        });
        String token = issued.getInvitationLink().split("#invitationToken=", 2)[1];
        request.setInvitationToken(token);
        Long invitedCompany = invitations.preview(token).getCompanyId();
        request.setCompanyId(invitedCompany);
        String bearer = jwt.createToken(member.getEmail(), java.util.List.of(member.getRole().getRoleType()));
        long sessionsBefore = sessions.count();
        http.perform(post("/auth/signup").header("Authorization", "Bearer " + bearer)
                .contentType("application/json").content(json.writeValueAsString(request)))
                .andExpect(status().isOk());
        tx.executeWithoutResult(status -> {
            em.clear();
            User created = users.findByEmailIgnoreCase(request.getEmail()).orElseThrow();
            assertEquals(invitedCompany, created.getCompany().getId());
            assertNotEquals(originalCompany, created.getCompany().getId());
            assertEquals(RoleCode.VIEW_ONLY, created.getRole().getCode());
            assertFalse(created.isOwnsCompany()); assertTrue(created.isEnabled());
            assertEquals(1, users.findByCompany_Id(originalCompany).size());
        });
        assertEquals(sessionsBefore + 1, sessions.count());
        assertThrows(CustomException.class, () -> invitations.preview(token));
    }

    @Test void failedSignupRestoresCallerAndCreatesNoAccountOrSession() {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                CustomUserDetail.builder().user(member).build(), null, java.util.List.of()));
        SecurityContextHolder.setContext(context);
        long accountsBefore = users.count(), sessionsBefore = sessions.count(), companiesBefore = companies.count();
        request.setCompanyId(originalCompany);
        assertThrows(CustomException.class, () -> service.signup(request));
        assertSame(context, SecurityContextHolder.getContext());
        assertEquals(accountsBefore, users.count()); assertEquals(sessionsBefore, sessions.count());
        assertEquals(companiesBefore, companies.count());
    }

    private void assertIndependentCompany(boolean email) {
        tx.executeWithoutResult(status -> {
            em.clear();
            User created = users.findByEmailIgnoreCase(request.getEmail()).orElseThrow();
            assertNotEquals(originalCompany, created.getCompany().getId());
            assertEquals(companyName, created.getCompany().getName());
            assertEquals(RoleCode.ADMIN, created.getRole().getCode());
            assertTrue(created.isOwnsCompany()); assertEquals(!email, created.isEnabled());
            assertEquals(1, users.findByCompany_Id(originalCompany).size());
            assertEquals(RoleCode.VIEW_ONLY, users.findById(member.getId()).orElseThrow().getRole().getCode());
        });
    }
}
