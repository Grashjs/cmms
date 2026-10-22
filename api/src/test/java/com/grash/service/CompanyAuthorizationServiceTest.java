package com.grash.service;

import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.enums.*;
import com.grash.repository.*;
import com.grash.utils.Helper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyAuthorizationServiceTest {
    @Mock UserRepository users;
    @Mock RoleRepository roles;
    @Mock CompanyRepository companies;
    @Mock UserInvitationRepository invitations;
    @Mock RefreshTokenRepository refreshTokens;
    @Mock CacheService cache;
    @Mock EntityManager em;
    @InjectMocks CompanyAuthorizationService policy;
    Company company;
    User actor;
    User target;
    Role admin;

    @BeforeEach void setup() {
        company = new Company(); company.setId(1L); company.getCompanySettings().setId(10L);
        admin = Helper.getDefaultRoles().get(0); admin.setId(1L);
        actor = user(1, admin); target = user(2, custom(2));
    }

    Role custom(long id) {
        return Role.builder().id(id).roleType(RoleType.ROLE_CLIENT).code(RoleCode.USER_CREATED)
                .companySettings(company.getCompanySettings()).name("Custom").build();
    }
    User user(long id, Role role) {
        User user = new User(); user.setId(id); user.setEnabled(true); user.setCompany(company);
        user.setRole(role); user.setEmail("person" + id + "@example.com"); return user;
    }

    @ParameterizedTest @ValueSource(strings = {"createPermissions", "viewPermissions", "viewOtherPermissions",
            "editOtherPermissions", "deleteOtherPermissions"})
    void everyPermissionSetIsComparedWithoutUnioningThem(String set) {
        Role owned = custom(3); Role requested = custom(4);
        ReflectionTestUtils.setField(requested, set, Set.of(PermissionEntity.FILES));
        assertFalse(policy.dominates(owned, requested));
        ReflectionTestUtils.setField(owned, set, Set.of(PermissionEntity.FILES, PermissionEntity.ASSETS));
        assertTrue(policy.dominates(owned, requested));
        ReflectionTestUtils.setField(owned, set, null);
        assertFalse(policy.dominates(owned, requested));
    }

    @Test void identicalListsDoNotConferLimitedAdminRequestApproval() {
        Role limitedAdmin = Helper.getDefaultRoles().get(1);
        Role clone = Helper.getDefaultRoles().get(1); clone.setCode(RoleCode.USER_CREATED);
        assertFalse(policy.dominates(clone, limitedAdmin));
        clone.getViewPermissions().add(PermissionEntity.SETTINGS);
        assertTrue(policy.dominates(clone, limitedAdmin));
        assertTrue(policy.dominates(admin, limitedAdmin));
    }

    @Test void identicalListsDoNotConferAccountAdministration() {
        Role clone = Helper.getDefaultRoles().get(0); clone.setCode(RoleCode.USER_CREATED);
        assertFalse(policy.dominates(clone, admin));
        assertTrue(policy.dominates(admin, clone));
    }

    @Test void onlyExplicitBuiltInClientRolesAreGlobal() {
        assertTrue(policy.roleInCompany(admin, company));
        Role unscoped = custom(2); unscoped.setCompanySettings(null);
        assertFalse(policy.roleInCompany(unscoped, company));
        admin.setRoleType(RoleType.ROLE_SUPER_ADMIN);
        assertFalse(policy.roleInCompany(admin, company));
    }

    @Test void foreignUserAndCustomRoleAreDenied() {
        Company other = new Company(); other.setId(99L); other.getCompanySettings().setId(99L);
        target.setCompany(other);
        assertThrows(CustomException.class, () -> policy.assertCanManage(actor, target));
        target.getRole().setCompanySettings(other.getCompanySettings());
        assertThrows(CustomException.class, () -> policy.assertCanInvite(actor, target.getRole()));
    }

    @Test void ownerIsProtectedEvenFromAnotherOwnerFlaggedAccount() {
        actor.setOwnsCompany(true); target.setOwnsCompany(true);
        assertThrows(CustomException.class, () -> policy.assertCanManage(actor, target));
        assertThrows(CustomException.class, () -> policy.assertCanChangePassword(actor, target));
        assertThrows(CustomException.class, () -> policy.assertCanDelete(actor, target));
    }

    @Test void deletionRequiresItsOwnPermissionAndStillChecksCurrentPrivileges() {
        Role limited = custom(3);
        limited.setDeleteOtherPermissions(Set.of(PermissionEntity.PEOPLE_AND_TEAMS));
        actor.setRole(limited);
        assertDoesNotThrow(() -> policy.assertCanDelete(actor, target));
        target.setRole(admin);
        assertThrows(CustomException.class, () -> policy.assertCanDelete(actor, target));
        limited.setDeleteOtherPermissions(Set.of());
        assertDoesNotThrow(() -> policy.assertCanDelete(actor, actor));
    }

    @Test void cannotChangeOwnRoleButCanChangeOwnPassword() {
        assertThrows(CustomException.class, () -> policy.assertCanChangeRole(actor, actor, target.getRole()));
        actor.setOwnsCompany(true);
        assertDoesNotThrow(() -> policy.assertCanChangePassword(actor, actor));
    }

    @Test void peopleProfileEditingAndCreationDoNotAuthorizePasswordReset() {
        Role manager = custom(3); manager.setEditOtherPermissions(Set.of(PermissionEntity.PEOPLE_AND_TEAMS));
        actor.setRole(manager); target.setCreatedBy(actor.getId());
        assertDoesNotThrow(() -> policy.assertCanEditProfile(actor, target));
        assertThrows(CustomException.class, () -> policy.assertCanChangePassword(actor, target));
        manager.setEditOtherPermissions(Set.of());
        assertDoesNotThrow(() -> policy.assertCanEditProfile(actor, target));
        assertThrows(CustomException.class, () -> policy.assertCanChangePassword(actor, target));
    }

    @Test void companyAdministratorCanManageSubordinateAndPeerAdministrator() {
        assertDoesNotThrow(() -> policy.assertCanInvite(actor, target.getRole()));
        assertDoesNotThrow(() -> policy.assertCanChangePassword(actor, target));
        assertDoesNotThrow(() -> policy.assertCanChangeRole(actor, target, admin));
        target.setRole(admin);
        assertDoesNotThrow(() -> policy.assertCanChangePassword(actor, target));
    }

    @Test void disabledOrPlatformActorCannotManageCompany() {
        actor.setEnabled(false);
        assertThrows(CustomException.class, () -> policy.assertCanInvite(actor, target.getRole()));
        actor.setEnabled(true); admin.setRoleType(RoleType.ROLE_SUPER_ADMIN);
        assertThrows(CustomException.class, () -> policy.assertCanInvite(actor, target.getRole()));
    }

    @Test void cannotEditOwnCustomRoleOrOwnersRole() {
        Role own = Helper.getDefaultRoles().get(0); own.setCode(RoleCode.USER_CREATED);
        own.setId(8L); own.setCompanySettings(company.getCompanySettings()); actor.setRole(own);
        assertThrows(CustomException.class, () -> policy.assertCanEditRole(actor, own, own, "edit"));
        target.setOwnsCompany(true);
        when(users.findByRole_Id(target.getRole().getId())).thenReturn(List.of(target));
        assertThrows(CustomException.class, () -> policy.assertCanEditRole(actor, target.getRole(), target.getRole(), "edit"));
    }

    @Test void permissionRevocationInvalidatesSessionsAndPendingInvitations() {
        policy.authorizationChanged(target);
        assertNotNull(target.getSessionInvalidatedAt());
        verify(invitations).revokeByIssuer(eq(target.getId()), any(Date.class));
        verify(refreshTokens).revokeAllByUser(target.getId());
        verify(cache).evictUserFromCache(target.getEmail());
    }

    @Test void customRoleWithALegacyForeignHolderCannotBeMutated() {
        Company other = new Company(); other.setId(99L); target.setCompany(other);
        when(users.findByRole_Id(target.getRole().getId())).thenReturn(List.of(target));
        assertThrows(CustomException.class, () -> policy.assertCanEditRole(actor, target.getRole(), target.getRole(), "edit"));
        verifyNoInteractions(invitations, refreshTokens);
    }
}
