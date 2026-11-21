package com.grash.service;

import com.grash.dto.RolePatchDTO;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.exception.CustomException;
import com.grash.mapper.RoleMapper;
import com.grash.model.Role;
import com.grash.model.User;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PlanFeatures;
import com.grash.model.enums.RoleCode;
import com.grash.model.enums.RoleType;
import com.grash.repository.RoleRepository;
import com.grash.utils.Helper;
import com.grash.utils.Sanitizer;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;
    private final LicenseService licenseService;
    private final CompanyAuthorizationService authorization;

    @Transactional
    public Role create(Role role) {
        return create(role, authorization.currentActor());
    }

    @Transactional
    public Role create(Role roleReq, User user) {
        user = authorization.actor(user);
        authorization.assertRoleOperation(user, "create");
        if (roleReq.getId() != null || roleReq.getRoleType() != RoleType.ROLE_CLIENT)
            throw new CustomException("Company management can only create new company roles", HttpStatus.FORBIDDEN);
        if (!user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.ROLE)
                || !licenseService.hasEntitlement(LicenseEntitlement.CUSTOM_ROLES))
            throw new CustomException("Your subscription and license must allow custom roles", HttpStatus.FORBIDDEN);
        roleReq.setPaid(true);
        roleReq.setCode(RoleCode.USER_CREATED);
        roleReq.setCompanySettings(user.getCompany().getCompanySettings());
        authorization.assertCanGrant(user, roleReq);
        Sanitizer.sanitizeRole(roleReq);
        return roleRepository.save(roleReq);
    }

    @Transactional
    public Role patch(Long id, RolePatchDTO patch, User user) {
        user = authorization.actor(user);
        Role current = authorization.role(Role.builder().id(id).build());
        // Validate a detached candidate before touching a managed entity or its collections.
        Role candidate = Role.builder().id(current.getId()).name(current.getName()).description(current.getDescription())
                .externalId(current.getExternalId()).roleType(current.getRoleType()).code(current.getCode())
                .paid(current.isPaid()).companySettings(current.getCompanySettings())
                .createPermissions(new HashSet<>(current.getCreatePermissions()))
                .viewPermissions(new HashSet<>(current.getViewPermissions()))
                .viewOtherPermissions(new HashSet<>(current.getViewOtherPermissions()))
                .editOtherPermissions(new HashSet<>(current.getEditOtherPermissions()))
                .deleteOtherPermissions(new HashSet<>(current.getDeleteOtherPermissions())).build();
        roleMapper.updateRole(candidate, patch);
        authorization.assertCanEditRole(user, current, candidate, "edit");
        boolean permissionsChanged = !authorization.fingerprint(current).equals(authorization.fingerprint(candidate));
        roleMapper.updateRole(current, patch);
        Sanitizer.sanitizeRole(current);
        Role saved = roleRepository.save(current);
        if (permissionsChanged) authorization.roleChanged(saved);
        return saved;
    }

    @Transactional
    public Role update(Long id, RolePatchDTO patch) {
        return patch(id, patch, authorization.currentActor());
    }

    @Transactional
    public List<Role> assignable(User requester, String operation, Long targetId) {
        User actor = authorization.actor(requester);
        if (!Set.of("invite", "role").contains(operation))
            throw new CustomException("Unknown assignment operation", HttpStatus.BAD_REQUEST);
        if ("role".equals(operation) && targetId == null)
            throw new CustomException("A target user is required", HttpStatus.BAD_REQUEST);
        User target = "role".equals(operation) ? authorization.target(targetId, actor) : null;
        return findByCompany(actor.getCompany().getId()).stream().filter(role -> {
            try {
                if (target == null) authorization.assertCanInvite(actor, role);
                else authorization.assertCanChangeRole(actor, target, role);
                return true;
            } catch (CustomException denied) { return false; }
        }).toList();
    }

    public Collection<Role> getAll() {
        return roleRepository.findAll();
    }

    public Collection<Role> getAll(User user) {
        if (user.getRole().getRoleType().equals(RoleType.ROLE_CLIENT)) {
            if (user.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS)) {
                return findByCompany(user.getCompany().getId());
            } else throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        } else return getAll();
    }

    @Transactional
    public void delete(Long id) {
        deleteByIdAndUser(id, authorization.currentActor());
    }

    @Transactional
    public void deleteByIdAndUser(Long id, User user) {
        user = authorization.actor(user);
        Role current = authorization.role(Role.builder().id(id).build());
        authorization.assertCanEditRole(user, current, current, "delete");
        // The database prevents deleting assigned roles; pending links are invalidated too.
        authorization.roleChanged(current);
        roleRepository.delete(current);
    }

    public Optional<Role> findById(Long id) {
        return roleRepository.findById(id);
    }

    public Role getById(Long id, User user) {
        Optional<Role> optionalRole = findById(id);
        if (optionalRole.isPresent()) {
            Role savedRole = optionalRole.get();
            if (user.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS) && savedRole.belongsToCompany(user.getCompany())) {
                return savedRole;
            } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public List<Role> findByCodeAndRoleType(RoleCode code, RoleType roleType) {
        return roleRepository.findByCodeAndRoleType(code, roleType);
    }

    public List<Role> findByCompany(Long id) {
        List<Role> result = findDefaultRoles();
        result.addAll(roleRepository.findByCompany_Id(id));
        return result;
    }

    public List<Role> findDefaultRoles() {
        return roleRepository.findDefaultRoles();
    }

    public Optional<Role> findDefaultRoleWithCode(RoleCode code) {
        return roleRepository.findDefaultRoleWithCode(code);
    }

    public List<Role> saveAll(List<Role> roles) {
        return roleRepository.saveAll(roles);
    }

    public void updateDefaultRoles() {
        List<Role> rolesToUpdate = new ArrayList<>();

        // Iterate through each tenant type's roles to find roles that need updates or additions
        List<Role> upToDateRoles = Helper.getDefaultRoles();
        List<Role> existingDefaultRoles = findDefaultRoles();
        List<Role> rolesToAdd =
                new ArrayList<>(upToDateRoles.stream().filter(upToDateRole -> existingDefaultRoles.stream().noneMatch(existingDefaultRole -> existingDefaultRole.getCode().equals(upToDateRole.getCode()))).toList());

        // Update roles by comparing privileges and 'paid' status between default and up-to-date roles
        for (Role existingDefaultRole : existingDefaultRoles) {
            for (Role upToDateRole : upToDateRoles) {
                if (existingDefaultRole.getCode().equals(upToDateRole.getCode())) {

                    // If privileges or 'paid' status differ, update default role
                    if (!CollectionUtils.isEqualCollection(existingDefaultRole.getCreatePermissions(),
                            upToDateRole.getCreatePermissions())
                            || !CollectionUtils.isEqualCollection(existingDefaultRole.getViewPermissions(),
                            upToDateRole.getViewPermissions())
                            || !CollectionUtils.isEqualCollection(existingDefaultRole.getViewOtherPermissions(),
                            upToDateRole.getViewOtherPermissions())
                            || !CollectionUtils.isEqualCollection(existingDefaultRole.getEditOtherPermissions(),
                            upToDateRole.getEditOtherPermissions())
                            || !CollectionUtils.isEqualCollection(existingDefaultRole.getDeleteOtherPermissions(),
                            upToDateRole.getDeleteOtherPermissions())
                            || existingDefaultRole.isPaid() != upToDateRole.isPaid()) {

                        // Clear and update privileges, and set 'paid' status
                        existingDefaultRole.getCreatePermissions().clear();
                        existingDefaultRole.getCreatePermissions().addAll(upToDateRole.getCreatePermissions());
                        existingDefaultRole.getViewPermissions().clear();
                        existingDefaultRole.getViewPermissions().addAll(upToDateRole.getViewPermissions());
                        existingDefaultRole.getViewOtherPermissions().clear();
                        existingDefaultRole.getViewOtherPermissions().addAll(upToDateRole.getViewOtherPermissions());
                        existingDefaultRole.getEditOtherPermissions().clear();
                        existingDefaultRole.getEditOtherPermissions().addAll(upToDateRole.getEditOtherPermissions());
                        existingDefaultRole.getDeleteOtherPermissions().clear();
                        existingDefaultRole.getDeleteOtherPermissions().addAll(upToDateRole.getDeleteOtherPermissions());
                        existingDefaultRole.setPaid(upToDateRole.isPaid());

                        // Add role to rolesToUpdate
                        rolesToUpdate.add(existingDefaultRole);
                    }
                    // Role matched, break loop to avoid redundant checks
                    break;
                }
            }
        }

        // Save any updated roles to the database
        if (!rolesToUpdate.isEmpty()) saveAll(rolesToUpdate);
        // Save any new roles to the database
        if (!rolesToAdd.isEmpty()) saveAll(rolesToAdd);
    }
}
