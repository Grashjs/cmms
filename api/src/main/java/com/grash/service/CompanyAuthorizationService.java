package com.grash.service;

import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.enums.*;
import com.grash.repository.*;
import com.grash.security.CustomUserDetail;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/** The common authorization boundary for company people, roles and invitation services. */
@Service
@RequiredArgsConstructor
public class CompanyAuthorizationService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final CompanyRepository companies;
    private final UserInvitationRepository invitations;
    private final RefreshTokenRepository refreshTokens;
    private final CacheService cache;
    private final EntityManager em;

    public User currentActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetail principal))
            throw denied("Authentication required");
        return actor(principal.getUser());
    }

    /** All management writes and invitation acceptance take this lock before changing any rows.
     * Reload under the lock: a principal or a cached Role is never an authorization snapshot. */
    public User actor(User supplied) {
        if (supplied == null || supplied.getId() == null) throw denied("Authentication required");
        User actor = users.findById(supplied.getId()).orElseThrow(() -> denied("Account unavailable"));
        if (actor.getCompany() == null) throw denied("Company membership required");
        lockCompany(actor.getCompany().getId());
        refresh(actor);
        refresh(actor.getRole());
        assertActor(actor);
        return actor;
    }

    public void lockCompany(Long id) {
        if (id == null || companies.lockAuthorization(id).isEmpty()) throw denied("Company unavailable");
    }

    public Role role(Role supplied) {
        if (supplied == null || supplied.getId() == null) throw denied("Role unavailable");
        Role role = roles.findById(supplied.getId()).orElseThrow(() -> new CustomException("Role unavailable", HttpStatus.NOT_FOUND));
        refresh(role);
        return role;
    }

    public User target(Long id, User actor) {
        User target = users.findByIdAndCompany_Id(id, actor.getCompany().getId())
                .orElseThrow(() -> new CustomException("User not found in your company", HttpStatus.NOT_FOUND));
        refresh(target);
        refresh(target.getRole());
        return target;
    }

    private void refresh(Object entity) {
        if (entity != null && em.contains(entity)) em.refresh(entity);
    }

    public void assertActor(User actor) {
        if (actor == null || actor.getId() == null || !actor.isEnabled() || !actor.isEnabledInSubscription()
                || actor.getCompany() == null || actor.getCompany().getId() == null
                || !roleInCompany(actor.getRole(), actor.getCompany()))
            throw denied("An active company account is required");
    }

    public boolean roleInCompany(Role role, Company company) {
        if (role == null || role.getRoleType() != RoleType.ROLE_CLIENT || role.getCode() == null
                || company == null || company.getId() == null) return false;
        if (role.getCompanySettings() == null) {
            // USER_CREATED with a missing tenant is never a global role.
            return EnumSet.of(RoleCode.ADMIN, RoleCode.LIMITED_ADMIN, RoleCode.TECHNICIAN,
                    RoleCode.LIMITED_TECHNICIAN, RoleCode.VIEW_ONLY, RoleCode.REQUESTER).contains(role.getCode());
        }
        return role.getCode() == RoleCode.USER_CREATED && role.getCompanySettings().getCompany() != null
                && Objects.equals(company.getId(), role.getCompanySettings().getCompany().getId());
    }

    public void assertCanGrant(User actor, Role role) {
        assertActor(actor);
        if (!roleInCompany(role, actor.getCompany())) throw denied("This role is not available in your company");
        if (!dominates(actor.getRole(), role)) throw denied("You cannot grant or manage privileges you do not have");
    }

    public boolean dominates(Role owned, Role requested) {
        if (owned == null || requested == null || owned.getRoleType() != RoleType.ROLE_CLIENT
                || requested.getRoleType() != RoleType.ROLE_CLIENT) return false;
        return contains(owned.getCreatePermissions(), requested.getCreatePermissions())
                && contains(owned.getViewPermissions(), requested.getViewPermissions())
                && contains(owned.getViewOtherPermissions(), requested.getViewOtherPermissions())
                && contains(owned.getEditOtherPermissions(), requested.getEditOtherPermissions())
                && contains(owned.getDeleteOtherPermissions(), requested.getDeleteOtherPermissions())
                && implicitPrivileges(owned).containsAll(implicitPrivileges(requested));
    }

    private boolean contains(Collection<PermissionEntity> owned, Collection<PermissionEntity> requested) {
        return owned != null && requested != null && owned.containsAll(requested);
    }

    private Set<String> implicitPrivileges(Role role) {
        Set<String> result = new HashSet<>();
        // RequestService approves/rejects requests for LIMITED_ADMIN even without SETTINGS.
        if (role.getCode() == RoleCode.LIMITED_ADMIN
                || (role.getViewPermissions() != null && role.getViewPermissions().contains(PermissionEntity.SETTINGS)))
            result.add("REVIEW_REQUESTS");
        if (role.getCode() == RoleCode.ADMIN) result.add("ACCOUNT_ADMINISTRATION");
        return result;
    }

    public void assertInviteOperation(User actor) {
        assertActor(actor);
        require(actor.getRole().getCreatePermissions(), PermissionEntity.PEOPLE_AND_TEAMS);
    }

    public void assertCanInvite(User actor, Role role) {
        assertInviteOperation(actor);
        assertCanGrant(actor, role);
    }

    public void assertCanManage(User actor, User target) {
        assertActor(actor);
        require(actor.getRole().getEditOtherPermissions(), PermissionEntity.PEOPLE_AND_TEAMS);
        assertManageableTarget(actor, target);
    }

    public void assertCanDelete(User actor, User target) {
        assertActor(actor);
        assertManageableTarget(actor, target);
        // Keep voluntary account deletion separate from managing other members.
        if (!Objects.equals(actor.getId(), target.getId()))
            require(actor.getRole().getDeleteOtherPermissions(), PermissionEntity.PEOPLE_AND_TEAMS);
    }

    private void assertManageableTarget(User actor, User target) {
        if (target == null || target.getCompany() == null
                || !Objects.equals(actor.getCompany().getId(), target.getCompany().getId()))
            throw denied("User not found in your company");
        if (target.isOwnsCompany() && !Objects.equals(actor.getId(), target.getId()))
            throw denied("Only the company owner can change this account");
        assertCanGrant(actor, target.getRole());
    }

    public void assertCanChangeRole(User actor, User target, Role requested) {
        if (Objects.equals(actor.getId(), target.getId())) throw denied("You cannot change your own role");
        assertCanManage(actor, target);
        assertCanGrant(actor, requested);
    }

    public void assertCanChangePassword(User actor, User target) {
        assertActor(actor);
        if (Objects.equals(actor.getId(), target.getId())) return;
        assertCanManage(actor, target);
        if (!implicitPrivileges(actor.getRole()).contains("ACCOUNT_ADMINISTRATION"))
            throw denied("Only a company administrator can change another user's password. Use account recovery instead.");
    }

    public void assertCanEditProfile(User actor, User target) {
        assertActor(actor);
        if (target.getCompany() == null || !Objects.equals(actor.getCompany().getId(), target.getCompany().getId())
                || !target.canBeEditedBy(actor)) throw denied("You cannot edit this profile");
    }

    public void assertRoleOperation(User actor, String operation) {
        assertActor(actor);
        require(actor.getRole().getViewPermissions(), PermissionEntity.SETTINGS);
        require(switch (operation) {
            case "create" -> actor.getRole().getCreatePermissions();
            case "delete" -> actor.getRole().getDeleteOtherPermissions();
            default -> actor.getRole().getEditOtherPermissions();
        }, PermissionEntity.SETTINGS);
    }

    public void assertCanEditRole(User actor, Role current, Role proposed, String operation) {
        assertRoleOperation(actor, operation);
        if (current.getCompanySettings() == null || current.getCode() != RoleCode.USER_CREATED)
            throw denied("Built-in roles cannot be changed");
        assertCanGrant(actor, current);
        assertCanGrant(actor, proposed);
        if (Objects.equals(actor.getRole().getId(), current.getId()))
            throw denied("You cannot change your own role's permissions");
        for (User holder : users.findByRole_Id(current.getId())) {
            if (holder.getCompany() == null || !Objects.equals(actor.getCompany().getId(), holder.getCompany().getId()))
                throw denied("This role has an inconsistent company assignment. An administrator must review it.");
            if (holder.isOwnsCompany())
                throw denied("The company owner's role cannot be changed by another member");
        }
    }

    private void require(Collection<PermissionEntity> permissions, PermissionEntity entity) {
        if (permissions == null || !permissions.contains(entity)) throw denied("You do not have permission for this operation");
    }

    /** Permanent revocation prevents a withdrawn-then-restored grant from reviving an old link. */
    public User authorizationChanged(User user) {
        user.setSessionInvalidatedAt(new Date());
        invitations.revokeByIssuer(user.getId(), new Date());
        refreshTokens.revokeAllByUser(user.getId());
        users.save(user);
        evictAfterCommit(user.getEmail());
        return user;
    }

    public void roleChanged(Role role) {
        invitations.revokeByRole(role.getId(), new Date());
        users.findByRole_Id(role.getId()).forEach(this::authorizationChanged);
    }

    public void evictAfterCommit(String email) {
        cache.evictUserFromCache(email);
        if (TransactionSynchronizationManager.isSynchronizationActive())
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { cache.evictUserFromCache(email); }
            });
    }

    /** Canonical snapshot includes all five sets, implicit code privileges and tenant scope. */
    public String fingerprint(Role role) {
        StringBuilder value = new StringBuilder("v1|").append(role.getId()).append('|')
                .append(role.getRoleType()).append('|').append(role.getCode()).append('|')
                .append(role.getCompanySettings() == null ? "global" : role.getCompanySettings().getId());
        for (Collection<PermissionEntity> set : Arrays.asList(role.getCreatePermissions(), role.getViewPermissions(),
                role.getViewOtherPermissions(), role.getEditOtherPermissions(), role.getDeleteOtherPermissions()))
            value.append('|').append(set == null ? "invalid" : set.stream().map(Enum::name).sorted().toList());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }

    private CustomException denied(String message) { return new CustomException(message, HttpStatus.FORBIDDEN); }
}
