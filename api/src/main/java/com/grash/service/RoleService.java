package com.grash.service;

import com.grash.dto.RolePatchDTO;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.exception.CustomException;
import com.grash.mapper.RoleMapper;
import com.grash.model.Company;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;
    private final CompanySettingsService companySettingsService;
    private final LicenseService licenseService;

    public Role create(Role role) {
        if (role.getCode().equals(RoleCode.USER_CREATED) && !licenseService.hasEntitlement(LicenseEntitlement.CUSTOM_ROLES))
            throw new CustomException("You need a license to create custom roles", HttpStatus.FORBIDDEN);
        Sanitizer.sanitizeRole(role);
        return roleRepository.save(role);
    }

    @Transactional
    public Role create(Role roleReq, User user) {
        if (user.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS)
                && user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.ROLE)) {
            assertCanGrant(roleReq.getCreatePermissions(), user.getRole().getCreatePermissions());
            assertCanGrant(roleReq.getViewPermissions(), user.getRole().getViewPermissions());
            assertCanGrant(roleReq.getViewOtherPermissions(), user.getRole().getViewOtherPermissions());
            assertCanGrant(roleReq.getEditOtherPermissions(), user.getRole().getEditOtherPermissions());
            assertCanGrant(roleReq.getDeleteOtherPermissions(), user.getRole().getDeleteOtherPermissions());
            roleReq.setPaid(true);
            roleReq.setCode(RoleCode.USER_CREATED);
            roleReq.setRoleType(RoleType.ROLE_CLIENT);
            roleReq.setCompanySettings(user.getCompany().getCompanySettings());
            return create(roleReq);
        } else throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
    }

    @Transactional
    public Role patch(Long id, RolePatchDTO role, User user) {
        Optional<Role> optionalRole = findById(id);
        if (optionalRole.isPresent()) {
            Role savedRole = optionalRole.get();
            if (!savedRole.belongsOnlyToCompany(user.getCompany())) {
                throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            }
            assertCanGrant(role.getCreatePermissions(), user.getRole().getCreatePermissions());
            assertCanGrant(role.getViewPermissions(), user.getRole().getViewPermissions());
            assertCanGrant(role.getViewOtherPermissions(), user.getRole().getViewOtherPermissions());
            assertCanGrant(role.getEditOtherPermissions(), user.getRole().getEditOtherPermissions());
            assertCanGrant(role.getDeleteOtherPermissions(), user.getRole().getDeleteOtherPermissions());
            return update(id, role);
        } else throw new CustomException("Role not found", HttpStatus.NOT_FOUND);
    }

    private void assertCanGrant(Collection<PermissionEntity> requested, Collection<PermissionEntity> owned) {
        if (requested != null && owned != null && !owned.containsAll(requested)) {
            throw new CustomException("Cannot grant permissions you don't have", HttpStatus.FORBIDDEN);
        }
    }

    public Role update(Long id, RolePatchDTO role) {
        if (roleRepository.existsById(id)) {
            Role savedRole = roleRepository.findById(id).get();
            Role updatedRole = roleMapper.updateRole(savedRole, role);
            Sanitizer.sanitizeRole(updatedRole);
            return roleRepository.save(updatedRole);
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
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

    public void delete(Long id) {
        roleRepository.deleteById(id);
    }

    @Transactional
    public void deleteByIdAndUser(Long id, User user) {
        if (!user.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS)) {
            throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        }
        Optional<Role> optionalRole = findById(id);
        if (optionalRole.isPresent()) {
            Role savedRole = optionalRole.get();
            if (!savedRole.belongsOnlyToCompany(user.getCompany())) {
                throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            }
            delete(id);
        } else throw new CustomException("Role not found", HttpStatus.NOT_FOUND);
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
