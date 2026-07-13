package com.grash.service;

import com.grash.dto.RolePatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.RoleMapper;
import com.grash.model.Role;
import com.grash.model.enums.RoleCode;
import com.grash.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    public Collection<Role> findByCompany(Long id) {
        return roleRepository.findByCompany_Id(id);
    }

    public List<Role> findDefaultRoles() {
        return roleRepository.findDefaultRoles(RoleCode.USER_CREATED);
    }

    public List<Role> saveAll(List<Role> roles) {
        return roleRepository.saveAll(roles);
    }
}
