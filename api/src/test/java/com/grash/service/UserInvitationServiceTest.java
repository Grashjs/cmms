package com.grash.service;

import com.grash.dto.InvitationPreviewDTO;
import com.grash.dto.IssuedInvitationDTO;
import com.grash.exception.CustomException;
import com.grash.model.Company;
import com.grash.model.Role;
import com.grash.model.User;
import com.grash.model.UserInvitation;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleType;
import com.grash.model.enums.RoleCode;
import com.grash.repository.*;
import jakarta.persistence.EntityManager;
import com.grash.repository.UserInvitationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Date;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserInvitationServiceTest {
    @Mock
    private UserInvitationRepository repository;
    @Mock UserRepository users;
    @Mock RoleRepository roles;
    @Mock CompanyRepository companies;
    @Mock RefreshTokenRepository refreshTokens;
    @Mock CacheService cache;
    @Mock EntityManager em;
    @InjectMocks
    private UserInvitationService service;

    private Company company;
    private Role role;
    private User inviter;
    private UserInvitation invitation;
    private final String token = "a".repeat(43);

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(service, "frontendUrl", "https://atlas.example");
        company = new Company();
        company.setId(10L);
        company.setName("Inviting company");
        company.getCompanySettings().setId(11L);
        role = Role.builder().id(20L).name("Technician").roleType(RoleType.ROLE_CLIENT)
                .code(RoleCode.USER_CREATED).companySettings(company.getCompanySettings())
                .createPermissions(Set.of(PermissionEntity.PEOPLE_AND_TEAMS)).build();
        inviter = new User();
        inviter.setId(30L);
        inviter.setRole(role);
        inviter.setCompany(company);
        inviter.setEnabled(true);
        CompanyAuthorizationService authorization = new CompanyAuthorizationService(users, roles, companies,
                repository, refreshTokens, cache, em);
        ReflectionTestUtils.setField(service, "authorization", authorization);
        lenient().when(users.findById(inviter.getId())).thenReturn(Optional.of(inviter));
        lenient().when(roles.findById(role.getId())).thenReturn(Optional.of(role));
        lenient().when(companies.lockAuthorization(company.getId())).thenReturn(Optional.of(company));
        invitation = UserInvitation.builder().id(40L).email("invited@example.com").role(role)
                .company(company).tokenHash(hash(token))
                .expiresAt(new Date(System.currentTimeMillis() + 60_000)).invitedById(inviter.getId())
                .invitedRoleId(role.getId()).roleAuthorizationHash(authorization.fingerprint(role))
                .issuerAuthorizationHash(authorization.fingerprint(role)).build();
        lenient().when(repository.findByTokenHash(hash(token))).thenReturn(Optional.of(invitation));
    }

    @Test
    void issue_returnsUniqueUnpredictableLinksAndPersistsOnlyDigestsWithExplicitScope() throws Exception {
        long before = System.currentTimeMillis();
        IssuedInvitationDTO first = service.issue(" Invited@Example.COM ", role, inviter);
        IssuedInvitationDTO second = service.issue("invited@example.com", role, inviter);
        ArgumentCaptor<UserInvitation> captured = ArgumentCaptor.forClass(UserInvitation.class);
        verify(repository, times(2)).save(captured.capture());
        String firstToken = first.getInvitationLink().split("#invitationToken=", 2)[1];
        assertEquals(43, firstToken.length());
        assertTrue(firstToken.matches("[A-Za-z0-9_-]+"));
        assertNotEquals(first.getInvitationLink(), second.getInvitationLink());
        UserInvitation saved = captured.getAllValues().get(0);
        assertEquals(hash(firstToken), saved.getTokenHash());
        assertNotEquals(firstToken, saved.getTokenHash());
        assertEquals("invited@example.com", saved.getEmail());
        assertSame(company, saved.getCompany());
        assertSame(role, saved.getRole());
        assertEquals(inviter.getId(), saved.getInvitedById());
        assertEquals(role.getId(), saved.getInvitedRoleId());
        assertEquals(inviter.getId(), saved.getCreatedBy());
        assertTrue(saved.getExpiresAt().getTime() >= before + Duration.ofDays(7).toMillis());
        assertTrue(saved.getExpiresAt().getTime() <= System.currentTimeMillis() + Duration.ofDays(7).toMillis());
        assertNull(saved.getConsumedAt());
        verify(repository, times(2)).revokePending(eq(company.getId()), eq("invited@example.com"), any(Date.class));
    }

    @Test
    void issue_rejectsUnauthorizedInviterWithoutWritingAnything() {
        inviter.setRole(Role.builder().roleType(RoleType.ROLE_CLIENT).createPermissions(Set.of()).build());
        assertThrows(CustomException.class, () -> service.issue("invited@example.com", role, inviter));
        verifyNoInteractions(repository);
    }

    @Test
    void issue_rejectsRoleFromAnotherCompanyWithoutWritingAnything() {
        Company other = new Company();
        other.getCompanySettings().setId(99L);
        role.setCompanySettings(other.getCompanySettings());
        assertThrows(CustomException.class, () -> service.issue("invited@example.com", role, inviter));
        verifyNoInteractions(repository);
    }

    @Test
    void issue_rejectsSuperAdministratorRole() {
        role.setRoleType(RoleType.ROLE_SUPER_ADMIN);
        assertThrows(CustomException.class, () -> service.issue("invited@example.com", role, inviter));
        verifyNoInteractions(repository);
    }

    @Test
    void issue_acceptsGlobalClientRoleUsingInvitersCompany() {
        role.setCompanySettings(null);
        role.setCode(RoleCode.TECHNICIAN);
        assertDoesNotThrow(() -> service.issue("invited@example.com", role, inviter));
        verify(repository).save(argThat(saved -> saved.getCompany() == company));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"not-a-token", " ", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa/"})
    void lock_rejectsMissingOrMalformedTokenBeforeDatabaseLookup(String rawToken) {
        assertThrows(CustomException.class, () -> service.lockValidInvitation(rawToken));
        verifyNoInteractions(repository);
    }

    @Test
    void lock_rejectsUnknownToken() throws Exception {
        when(repository.findByTokenHashForUpdate(hash(token))).thenReturn(Optional.empty());
        assertThrows(CustomException.class, () -> service.lockValidInvitation(token));
        verify(repository, never()).save(any());
    }

    @Test
    void lock_returnsValidInvitation() throws Exception {
        lenient().when(repository.findByTokenHashForUpdate(hash(token))).thenReturn(Optional.of(invitation));
        assertSame(invitation, service.lockValidInvitation(token));
    }

    @Test
    void lock_rejectsExpiredInvitation() {
        invitation.setExpiresAt(new Date(System.currentTimeMillis() - 1));
        assertInvalidInvitation();
    }

    @Test
    void lock_rejectsAlreadyConsumedInvitation() {
        invitation.setConsumedAt(new Date());
        invitation.setAcceptedUserId(100L);
        assertInvalidInvitation();
    }

    @Test
    void lock_rejectsRevokedInvitation() {
        invitation.setRevokedAt(new Date());
        assertInvalidInvitation();
    }

    @Test
    void lock_rejectsLegacyInvitationWithNoDigestOrExpiry() {
        invitation.setTokenHash(null);
        invitation.setExpiresAt(null);
        assertInvalidInvitation();
    }

    @Test
    void lock_rejectsMissingCompany() {
        invitation.setCompany(null);
        assertInvalidInvitation();
    }

    @Test
    void lock_rejectsRoleMovedToAnotherCompany() {
        Company other = new Company();
        other.getCompanySettings().setId(99L);
        role.setCompanySettings(other.getCompanySettings());
        assertInvalidInvitation();
    }

    @Test
    void preview_exposesOnlyValidatedScopeWithoutConsumingInvitation() throws Exception {
        when(repository.findByTokenHash(hash(token))).thenReturn(Optional.of(invitation));
        InvitationPreviewDTO preview = service.preview(token);
        assertEquals(invitation.getEmail(), preview.getEmail());
        assertEquals(role.getId(), preview.getRoleId());
        assertEquals(company.getId(), preview.getCompanyId());
        assertEquals(company.getName(), preview.getCompanyName());
        verify(repository, never()).save(any());
        assertNull(invitation.getConsumedAt());
    }

    @Test
    void consume_recordsAcceptanceAndRetainsInvitationAudit() {
        User acceptedUser = acceptedUser();
        service.consume(invitation, acceptedUser);
        assertNotNull(invitation.getConsumedAt());
        assertEquals(acceptedUser.getId(), invitation.getAcceptedUserId());
        assertEquals(inviter.getId(), invitation.getInvitedById());
        verify(repository).save(invitation);
        verify(repository, never()).delete(any());
        assertThrows(CustomException.class, () -> service.consume(invitation, acceptedUser));
    }

    @Test
    void consume_rejectsDifferentEmailRoleOrCompany() {
        User acceptedUser = acceptedUser();
        acceptedUser.setEmail("other@example.com");
        assertThrows(CustomException.class, () -> service.consume(invitation, acceptedUser));
        acceptedUser.setEmail(invitation.getEmail());
        acceptedUser.setRole(Role.builder().id(999L).build());
        assertThrows(CustomException.class, () -> service.consume(invitation, acceptedUser));
        acceptedUser.setRole(role);
        Company other = new Company();
        other.setId(999L);
        acceptedUser.setCompany(other);
        assertThrows(CustomException.class, () -> service.consume(invitation, acceptedUser));
        verify(repository, never()).save(any());
    }

    @Test
    void lock_rejectsIssuerWhoseRightsWereRemovedOrAccountDisabled() {
        inviter.setEnabled(false);
        assertInvalidInvitation();
        inviter.setEnabled(true);
        role.setCreatePermissions(Set.of());
        assertInvalidInvitation();
    }

    @Test
    void lock_rejectsChangedGrantEvenWhenIssuerStillHasEnoughRights() {
        role.setViewPermissions(Set.of(PermissionEntity.FILES));
        assertInvalidInvitation();
    }

    @Test
    void lock_rejectsOldInvitationWithoutAuthorizationSnapshot() {
        invitation.setRoleAuthorizationHash(null);
        assertInvalidInvitation();
    }

    private User acceptedUser() {
        User user = new User();
        user.setId(100L);
        user.setEmail(invitation.getEmail());
        user.setRole(role);
        user.setCompany(company);
        return user;
    }

    private void assertInvalidInvitation() {
        lenient().when(repository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(invitation));
        assertThrows(CustomException.class, () -> service.lockValidInvitation(token));
        verify(repository, never()).save(any());
    }

    private String hash(String rawToken) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(rawToken.getBytes(StandardCharsets.UTF_8)));
    }
}
