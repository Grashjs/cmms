package com.grash.service;

import com.grash.dto.RolePatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.RoleMapper;
import com.grash.model.CompanySettings;
import com.grash.model.Company;
import com.grash.model.Role;
import com.grash.model.enums.RoleCode;
import com.grash.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RoleMapper roleMapper;

    @InjectMocks
    private RoleService roleService;

    private Role role;
    private Company company;
    private CompanySettings companySettings;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);

        companySettings = new CompanySettings();
        companySettings.setId(1L);
        companySettings.setCompany(company);

        role = new Role();
        role.setId(1L);
        role.setName("Test Role");
        role.setCompanySettings(companySettings);
    }

    @Test
    void create() {
        when(roleRepository.save(any(Role.class))).thenReturn(role);

        Role result = roleService.create(role);

        assertNotNull(result);
        assertEquals(role.getId(), result.getId());
        verify(roleRepository).save(role);
    }

    @Test
    void update_whenExists() {
        RolePatchDTO patchDTO = new RolePatchDTO();
        when(roleRepository.existsById(1L)).thenReturn(true);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(roleRepository.save(any(Role.class))).thenReturn(role);
        when(roleMapper.updateRole(any(Role.class), any(RolePatchDTO.class))).thenReturn(role);

        Role result = roleService.update(1L, patchDTO);

        assertNotNull(result);
        assertEquals(role.getId(), result.getId());
        verify(roleRepository).save(role);
    }

    @Test
    void update_whenNotExists_shouldThrowException() {
        RolePatchDTO patchDTO = new RolePatchDTO();
        when(roleRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class, () -> roleService.update(1L, patchDTO));

        assertEquals("Not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void getAll() {
        when(roleRepository.findAll()).thenReturn(Collections.singletonList(role));

        assertEquals(1, roleService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(roleRepository).deleteById(1L);
        roleService.delete(1L);
        verify(roleRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

        assertTrue(roleService.findById(1L).isPresent());
    }

    @Test
    void findByName() {
        when(roleRepository.findByName("Test Role")).thenReturn(Optional.of(role));

        assertTrue(roleService.findByName("Test Role").isPresent());
    }

    @Test
    void findByCompany() {
        when(roleRepository.findByCompany_Id(1L)).thenReturn(Collections.singletonList(role));

        assertEquals(1, roleService.findByCompany(1L).size());
    }

    @Test
    void findDefaultRoles() {
        when(roleRepository.findDefaultRoles(RoleCode.USER_CREATED)).thenReturn(Collections.singletonList(role));

        assertEquals(1, roleService.findDefaultRoles().size());
    }

    @Test
    void saveAll() {
        List<Role> roles = Collections.singletonList(role);
        when(roleRepository.saveAll(roles)).thenReturn(roles);

        List<Role> result = roleService.saveAll(roles);

        assertEquals(1, result.size());
    }
}
