package com.grash.service;

import com.grash.dto.RolePatchDTO;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.exception.CustomException;
import com.grash.mapper.RoleMapper;
import com.grash.model.Role;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleCode;
import com.grash.model.enums.RoleType;
import com.grash.repository.RoleRepository;
import com.grash.utils.Helper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @InjectMocks
    private RoleService roleService;

    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private CompanySettingsService companySettingsService;
    @Mock
    private LicenseService licenseService;

    private Role adminRole;
    private Role customRole;

    @BeforeEach
    void setUp() {
        adminRole = Role.builder()
                .id(1L)
                .name("Administrator")
                .roleType(RoleType.ROLE_CLIENT)
                .code(RoleCode.ADMIN)
                .paid(true)
                .createPermissions(new HashSet<>(Collections.singleton(PermissionEntity.WORK_ORDERS)))
                .viewPermissions(new HashSet<>(Collections.singleton(PermissionEntity.ASSETS)))
                .viewOtherPermissions(new HashSet<>())
                .editOtherPermissions(new HashSet<>())
                .deleteOtherPermissions(new HashSet<>())
                .build();

        customRole = Role.builder()
                .id(2L)
                .name("Custom")
                .roleType(RoleType.ROLE_CLIENT)
                .code(RoleCode.USER_CREATED)
                .paid(false)
                .build();
    }

    @Nested
    class Create {

        @Test
        void customRole_withoutEntitlement_throwsForbidden() {
            when(licenseService.hasEntitlement(LicenseEntitlement.CUSTOM_ROLES)).thenReturn(false);

            CustomException ex = assertThrows(CustomException.class, () -> roleService.create(customRole));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
            verify(roleRepository, never()).save(any(Role.class));
        }

        @Test
        void customRole_withEntitlement_saves() {
            when(licenseService.hasEntitlement(LicenseEntitlement.CUSTOM_ROLES)).thenReturn(true);
            when(roleRepository.save(customRole)).thenReturn(customRole);

            Role result = roleService.create(customRole);

            assertEquals(customRole, result);
            verify(roleRepository).save(customRole);
        }

        @Test
        void defaultRole_savesWithoutEntitlementCheck() {
            when(roleRepository.save(adminRole)).thenReturn(adminRole);

            Role result = roleService.create(adminRole);

            assertEquals(adminRole, result);
            verify(licenseService, never()).hasEntitlement(any());
            verify(roleRepository).save(adminRole);
        }
    }

    @Nested
    class Update {

        @Test
        void existingRole_updatesAndSaves() {
            RolePatchDTO patch = new RolePatchDTO();
            patch.setName("Updated");
            patch.setDescription("desc");
            Role updatedRole = Role.builder().id(1L).name("Updated").build();
            when(roleRepository.existsById(1L)).thenReturn(true);
            when(roleRepository.findById(1L)).thenReturn(Optional.of(adminRole));
            when(roleMapper.updateRole(adminRole, patch)).thenReturn(updatedRole);
            when(roleRepository.save(updatedRole)).thenReturn(updatedRole);

            Role result = roleService.update(1L, patch);

            assertEquals(updatedRole, result);
            verify(roleMapper).updateRole(adminRole, patch);
            verify(roleRepository).save(updatedRole);
        }

        @Test
        void nonExistingRole_throwsNotFound() {
            RolePatchDTO patch = new RolePatchDTO();
            when(roleRepository.existsById(99L)).thenReturn(false);

            CustomException ex = assertThrows(CustomException.class,
                    () -> roleService.update(99L, patch));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
            verify(roleRepository, never()).findById(anyLong());
            verify(roleRepository, never()).save(any(Role.class));
        }
    }

    @Nested
    class GetAll {

        @Test
        void returnsAllRoles() {
            when(roleRepository.findAll()).thenReturn(Arrays.asList(adminRole, customRole));

            Collection<Role> result = roleService.getAll();

            assertEquals(2, result.size());
            assertTrue(result.contains(adminRole));
            assertTrue(result.contains(customRole));
            verify(roleRepository).findAll();
        }
    }

    @Nested
    class Delete {

        @Test
        void deletesById() {
            roleService.delete(1L);

            verify(roleRepository).deleteById(1L);
        }
    }

    @Nested
    class FindById {

        @Test
        void existingId_returnsRole() {
            when(roleRepository.findById(1L)).thenReturn(Optional.of(adminRole));

            Optional<Role> result = roleService.findById(1L);

            assertTrue(result.isPresent());
            assertEquals(adminRole, result.get());
        }

        @Test
        void nonExistingId_returnsEmpty() {
            when(roleRepository.findById(99L)).thenReturn(Optional.empty());

            Optional<Role> result = roleService.findById(99L);

            assertFalse(result.isPresent());
        }
    }

    @Nested
    class FindByCompany {

        @Test
        void returnsDefaultPlusCompanyRoles() {
            Role companyRole = Role.builder()
                    .id(3L)
                    .name("Company Role")
                    .code(RoleCode.USER_CREATED)
                    .build();
            when(roleRepository.findDefaultRoles()).thenReturn(new ArrayList<>(List.of(adminRole)));
            when(roleRepository.findByCompany_Id(1L)).thenReturn(List.of(companyRole));

            List<Role> result = roleService.findByCompany(1L);

            assertEquals(2, result.size());
            assertTrue(result.contains(adminRole));
            assertTrue(result.contains(companyRole));
            verify(roleRepository).findDefaultRoles();
            verify(roleRepository).findByCompany_Id(1L);
        }
    }

    @Nested
    class FindDefaultRoles {

        @Test
        void returnsDefaultRoles() {
            when(roleRepository.findDefaultRoles()).thenReturn(new ArrayList<>(List.of(adminRole)));

            List<Role> result = roleService.findDefaultRoles();

            assertEquals(1, result.size());
            assertEquals(adminRole, result.get(0));
        }
    }

    @Nested
    class FindDefaultRoleWithCode {

        @Test
        void returnsRoleForCode() {
            when(roleRepository.findDefaultRoleWithCode(RoleCode.ADMIN)).thenReturn(Optional.of(adminRole));

            Optional<Role> result = roleService.findDefaultRoleWithCode(RoleCode.ADMIN);

            assertTrue(result.isPresent());
            assertEquals(RoleCode.ADMIN, result.get().getCode());
        }
    }

    @Nested
    class SaveAll {

        @Test
        void savesAllRoles() {
            List<Role> roles = List.of(adminRole, customRole);
            when(roleRepository.saveAll(roles)).thenReturn(roles);

            List<Role> result = roleService.saveAll(roles);

            assertEquals(2, result.size());
            verify(roleRepository).saveAll(roles);
        }
    }

    @Nested
    class UpdateDefaultRoles {

        private List<Role> upToDateRoles;

        @BeforeEach
        void init() {
            upToDateRoles = Helper.getDefaultRoles();
        }

        @Test
        void noExistingRoles_addsAllDefaults() {
            when(roleRepository.findDefaultRoles()).thenReturn(new ArrayList<>());

            roleService.updateDefaultRoles();

            verify(roleRepository).saveAll(argThat(roles -> ((List<Role>) roles).size() == upToDateRoles.size()));
        }

        @Test
        void existingRolesOutOfDate_updatesThem() {
            Role staleAdmin = Role.builder()
                    .id(10L)
                    .name("Administrator")
                    .roleType(RoleType.ROLE_CLIENT)
                    .code(RoleCode.ADMIN)
                    .paid(false)
                    .createPermissions(new HashSet<>(Collections.singleton(PermissionEntity.FILES)))
                    .viewPermissions(new HashSet<>())
                    .viewOtherPermissions(new HashSet<>())
                    .editOtherPermissions(new HashSet<>())
                    .deleteOtherPermissions(new HashSet<>())
                    .build();
            when(roleRepository.findDefaultRoles()).thenReturn(new ArrayList<>(List.of(staleAdmin)));

            roleService.updateDefaultRoles();

            Set<PermissionEntity> allEntities = new HashSet<>(Arrays.asList(PermissionEntity.values()));
            assertEquals(allEntities, staleAdmin.getCreatePermissions());
            assertEquals(allEntities, staleAdmin.getViewPermissions());
            assertEquals(allEntities, staleAdmin.getViewOtherPermissions());
            assertEquals(allEntities, staleAdmin.getEditOtherPermissions());
            assertEquals(allEntities, staleAdmin.getDeleteOtherPermissions());
            assertTrue(staleAdmin.isPaid());
            verify(roleRepository).saveAll(argThat(roles -> ((List<Role>) roles).contains(staleAdmin)));
        }

        @Test
        void existingRolesUpToDate_savesNothing() {
            List<Role> existing = new ArrayList<>();
            long id = 1L;
            for (Role upToDateRole : upToDateRoles) {
                existing.add(copyRoleWithId(upToDateRole, id++));
            }
            when(roleRepository.findDefaultRoles()).thenReturn(existing);

            roleService.updateDefaultRoles();

            verify(roleRepository, never()).saveAll(anyList());
        }

        @Test
        void partiallyOutOfDate_addsMissingAndUpdatesExisting() {
            Role staleViewOnly = Role.builder()
                    .id(11L)
                    .name("View Only")
                    .roleType(RoleType.ROLE_CLIENT)
                    .code(RoleCode.VIEW_ONLY)
                    .paid(true)
                    .createPermissions(new HashSet<>())
                    .viewPermissions(new HashSet<>())
                    .viewOtherPermissions(new HashSet<>())
                    .editOtherPermissions(new HashSet<>())
                    .deleteOtherPermissions(new HashSet<>())
                    .build();
            when(roleRepository.findDefaultRoles()).thenReturn(new ArrayList<>(List.of(staleViewOnly)));

            roleService.updateDefaultRoles();

            assertFalse(staleViewOnly.isPaid());
            verify(roleRepository, times(2)).saveAll(anyList());
        }

        private Role copyRoleWithId(Role source, long id) {
            return Role.builder()
                    .id(id)
                    .name(source.getName())
                    .roleType(source.getRoleType())
                    .code(source.getCode())
                    .paid(source.isPaid())
                    .createPermissions(new HashSet<>(source.getCreatePermissions()))
                    .viewPermissions(new HashSet<>(source.getViewPermissions()))
                    .viewOtherPermissions(new HashSet<>(source.getViewOtherPermissions()))
                    .editOtherPermissions(new HashSet<>(source.getEditOtherPermissions()))
                    .deleteOtherPermissions(new HashSet<>(source.getDeleteOtherPermissions()))
                    .build();
        }
    }
}
