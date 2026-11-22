package com.grash.service;

import com.grash.dto.*;
import com.grash.exception.CustomException;
import com.grash.mapper.RoleMapper;
import com.grash.mapper.UserMapper;
import com.grash.model.*;
import com.grash.model.enums.*;
import com.grash.repository.*;
import com.grash.security.CustomUserDetail;
import com.grash.utils.Helper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** These tests exercise the public service entry points, with no controller authorization. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PeopleAuthorizationRegressionTest {
    @Mock CompanyRepository companies;
    @Mock RefreshTokenRepository refreshTokens;
    @Mock UserRepository users;
    @Mock RoleRepository roles;
    @Mock UserInvitationRepository invitations;
    @Mock RoleService roleLookup;
    @Mock UserInvitationService invitationIssuer;
    @Mock UserMapper userMapper;
    @Mock RoleMapper roleMapper;
    @Mock PasswordEncoder passwords;
    @Mock CacheService cache;
    @Mock RefreshTokenService sessions;
    @Mock EntityManager em;
    @Mock LicenseService license;
    @InjectMocks UserService userService;
    @InjectMocks UserInvitationService invitationService;
    @InjectMocks RoleService roleService;

    Company company;
    User limited;
    User target;
    Role low;
    Role admin;

    @BeforeEach
    void setup() {
        company = new Company(); company.setId(100L); company.getCompanySettings().setId(101L);
        Subscription subscription = new Subscription(); subscription.setUsersCount(100);
        SubscriptionPlan plan = new SubscriptionPlan(); plan.setFeatures(Set.of(PlanFeatures.ROLE));
        subscription.setSubscriptionPlan(plan); company.setSubscription(subscription); company.setInvitedUsers(true);
        low = Role.builder().id(10L).code(RoleCode.USER_CREATED).roleType(RoleType.ROLE_CLIENT)
                .name("People manager").companySettings(company.getCompanySettings())
                .createPermissions(Set.of(PermissionEntity.PEOPLE_AND_TEAMS))
                .editOtherPermissions(Set.of(PermissionEntity.PEOPLE_AND_TEAMS)).build();
        admin = Helper.getDefaultRoles().get(0); admin.setId(20L); admin.setPaid(false);
        limited = user(1L, low); target = user(2L, low);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                CustomUserDetail.builder().user(limited).build(), "", List.of()));
        when(users.findByIdAndCompany_Id(target.getId(), company.getId())).thenReturn(Optional.of(target));
        when(users.findById(anyLong())).thenAnswer(i -> Optional.of(i.getArgument(0).equals(1L) ? limited : target));
        when(users.existsById(target.getId())).thenReturn(true);
        when(users.save(any())).thenAnswer(i -> i.getArgument(0));
        when(users.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(userMapper.updateUser(any(), any())).thenAnswer(i -> i.getArgument(0));
        when(roleMapper.updateRole(any(), any())).thenAnswer(i -> i.getArgument(0));
        when(roleLookup.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(roleLookup.findById(low.getId())).thenReturn(Optional.of(low));
        when(companies.lockAuthorization(company.getId())).thenReturn(Optional.of(company));
        when(roles.findById(low.getId())).thenReturn(Optional.of(low));
        when(roles.findById(admin.getId())).thenReturn(Optional.of(admin));
        CompanyAuthorizationService authorization = new CompanyAuthorizationService(users, roles, companies,
                invitations, refreshTokens, cache, em);
        for (Object service : List.of(userService, invitationService, roleService))
            ReflectionTestUtils.setField(service, "authorization", authorization);
        ReflectionTestUtils.setField(invitationService, "frontendUrl", "https://test.invalid");
    }

    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    User user(long id, Role role) {
        User user = new User(); user.setId(id); user.setCompany(company); user.setRole(role);
        user.setEnabled(true); user.setEmail("user" + id + "@example.com"); user.setPassword("unchanged");
        return user;
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void inviteUsers_cannotGrantAdministrator_evenWithEmailDisabled(boolean email) {
        ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", email);
        UserInvitationDTO request = new UserInvitationDTO(); request.setRole(admin);
        request.setEmails(List.of("invited@example.com"));
        assertThrows(CustomException.class, () -> userService.inviteUsers(request, limited));
        verifyNoInteractions(invitationIssuer);
    }

    @Test void directIssuance_cannotGrantAdministrator() {
        assertThrows(CustomException.class, () -> invitationService.issue("invited@example.com", admin, limited));
        verifyNoInteractions(invitations);
    }

    @Test void roleChange_cannotPromotePeer() {
        assertThrows(CustomException.class, () -> userService.patchUserRole(target.getId(), admin.getId(), limited));
        assertSame(low, target.getRole()); verify(users, never()).save(any());
    }

    @Test void roleChange_cannotDemoteAdministratorFirst() {
        target.setRole(admin);
        assertThrows(CustomException.class, () -> userService.patchUserRole(target.getId(), low.getId(), limited));
        assertSame(admin, target.getRole()); verify(users, never()).save(any());
    }

    @Test void roleChange_cannotTouchOwnerEvenWithAnEqualRole() {
        target.setOwnsCompany(true);
        assertThrows(CustomException.class, () -> userService.patchUserRole(target.getId(), low.getId(), limited));
        verify(users, never()).save(any());
    }

    @Test void passwordChange_cannotTakeOverAdministratorCreatedByActor() {
        target.setRole(admin); target.setCreatedBy(limited.getId());
        UserPatchDTO patch = new UserPatchDTO(); patch.setNewPassword("Harmless-test-password-42!");
        assertThrows(CustomException.class, () -> userService.update(target.getId(), patch));
        assertEquals("unchanged", target.getPassword()); verifyNoInteractions(passwords, sessions);
        verify(users, never()).saveAndFlush(any());
    }

    @Test void customRolePatch_requiresSettingsOperationPermission() {
        Role other = Role.builder().id(30L).name("Other custom role").code(RoleCode.USER_CREATED)
                .roleType(RoleType.ROLE_CLIENT).companySettings(company.getCompanySettings()).build();
        when(roles.findById(30L)).thenReturn(Optional.of(other)); when(roles.existsById(30L)).thenReturn(true);
        RolePatchDTO patch = new RolePatchDTO(); patch.setName("Renamed without permission");
        assertThrows(CustomException.class, () -> roleService.patch(30L, patch, limited));
        verify(roles, never()).save(any());
    }

    @Test void customRolePatch_cannotDemoteMorePrivilegedRole() {
        low.setViewPermissions(Set.of(PermissionEntity.SETTINGS));
        Role stronger = Role.builder().id(30L).name("Stronger").code(RoleCode.USER_CREATED)
                .roleType(RoleType.ROLE_CLIENT).companySettings(company.getCompanySettings())
                .deleteOtherPermissions(Set.of(PermissionEntity.WORK_ORDERS)).build();
        when(roles.findById(30L)).thenReturn(Optional.of(stronger)); when(roles.existsById(30L)).thenReturn(true);
        RolePatchDTO patch = new RolePatchDTO(); patch.setDeleteOtherPermissions(List.of());
        assertThrows(CustomException.class, () -> roleService.patch(30L, patch, limited));
        assertEquals(Set.of(PermissionEntity.WORK_ORDERS), stronger.getDeleteOtherPermissions());
        verify(roles, never()).save(any());
    }
    @ParameterizedTest @CsvSource({"self,false", "self,true", "admin-self,false", "admin-self,true",
            "admin-other,false", "admin-other,true"})
    void profilePasswordIsBlockedWithEmailBeforeAnyMutation(String mode, boolean explicitRequester) {
        ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", true);
        if (mode.startsWith("admin")) limited.setRole(admin);
        User subject = mode.equals("admin-other") ? target : limited;
        when(users.findByIdAndCompany_Id(limited.getId(), company.getId())).thenReturn(Optional.of(limited));
        UserPatchDTO patch = new UserPatchDTO();
        patch.setNewPassword("Blocked-password-42!"); patch.setFirstName("Must not change");

        CustomException exception = assertThrows(CustomException.class, () -> {
            if (explicitRequester) userService.update(subject.getId(), patch, limited);
            else userService.update(subject.getId(), patch);
        });

        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, exception.getHttpStatus());
        assertEquals("unchanged", subject.getPassword());
        assertNull(subject.getFirstName()); assertNull(subject.getSessionInvalidatedAt());
        verifyNoInteractions(passwords, sessions, userMapper, cache);
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void ownPasswordAndAdministratorResetRemainAllowedWithoutEmail() {
        ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", false);
        UserPatchDTO patch = new UserPatchDTO(); patch.setNewPassword("Legitimate-password-42!");
        when(passwords.encode(anyString())).thenReturn("new-hash");
        when(users.findByIdAndCompany_Id(limited.getId(), company.getId())).thenReturn(Optional.of(limited));
        userService.update(limited.getId(), patch, limited);
        assertEquals("new-hash", limited.getPassword());
        verify(sessions).revokeAllForUser(limited);
        limited.setRole(admin);
        userService.update(target.getId(), patch, limited);
        assertEquals("new-hash", target.getPassword());
        verify(sessions).revokeAllForUser(target);
    }

    @Test void administratorCanPromotePeerAndInviteThroughDirectIssuance() {
        limited.setRole(admin);
        assertSame(admin, userService.patchUserRole(target.getId(), admin.getId(), limited).getRole());
        assertNotNull(invitationService.issue("legitimate@example.com", low, limited).getInvitationLink());
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void cannotInviteLimitedAdminByCloningOnlyItsPermissionLists(boolean email) {
        ReflectionTestUtils.setField(userService, "enableInvitationViaEmail", email);
        Role requested = Helper.getDefaultRoles().get(1); requested.setId(99L); requested.setPaid(false);
        Role clone = Helper.getDefaultRoles().get(1); clone.setId(98L); clone.setCode(RoleCode.USER_CREATED);
        clone.setCompanySettings(company.getCompanySettings());
        clone.getCreatePermissions().add(PermissionEntity.PEOPLE_AND_TEAMS);
        limited.setRole(clone);
        when(roles.findById(99L)).thenReturn(Optional.of(requested));
        when(roleLookup.findById(99L)).thenReturn(Optional.of(requested));
        UserInvitationDTO request = new UserInvitationDTO(); request.setRole(requested);
        request.setEmails(List.of("implicit@example.com"));
        assertThrows(CustomException.class, () -> userService.inviteUsers(request, limited));
        assertThrows(CustomException.class, () -> invitationService.issue("implicit@example.com", requested, limited));
        verifyNoInteractions(invitationIssuer); verify(invitations, never()).save(any());
    }

}
