package com.grash.service;

import com.google.rpc.Help;
import com.grash.dto.RolePatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.RoleMapper;
import com.grash.model.Company;
import com.grash.model.Role;
import com.grash.model.enums.RoleCode;
import com.grash.repository.RoleRepository;
import com.grash.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;
    private final CompanySettingsService companySettingsService;

    public Role create(Role Role) {
        return roleRepository.save(Role);
    }

    public Role update(Long id, RolePatchDTO role) {
        if (roleRepository.existsById(id)) {
            Role savedRole = roleRepository.findById(id).get();
            return roleRepository.save(roleMapper.updateRole(savedRole, role));
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public Collection<Role> getAll() {
        return roleRepository.findAll();
    }

    public void delete(Long id) {
        roleRepository.deleteById(id);
    }

    public Optional<Role> findById(Long id) {
        return roleRepository.findById(id);
    }

    public Optional<Role> findByName(String name) {
        return roleRepository.findByName(name);
    }

    public List<Role> findByCompany(Long id) {
        List<Role> result = findDefaultRoles();
        result.addAll(roleRepository.findByCompany_Id(id));
        return result;
    }

    public List<Role> findDefaultRoles() {
        return roleRepository.findDefaultRoles();
    }

    public List<Role> saveAll(List<Role> roles) {
        return roleRepository.saveAll(roles);
    }

    public void updateDefaultRoles() {
        List<Role> rolesToUpdate = new ArrayList<>();
        List<Role> rolesToAdd = new ArrayList<>();

        // Iterate through each tenant type's roles to find roles that need updates or additions
        List<Role> upToDateRoles = Helper.getDefaultRoles();
        List<Role> existingDefaultRoles = findDefaultRoles();
        rolesToAdd.addAll(upToDateRoles.stream().filter(upToDateRole -> existingDefaultRoles.stream().noneMatch(existingDefaultRole -> existingDefaultRole.getCode().equals(upToDateRole.getCode()))).collect(Collectors.toList()));

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
