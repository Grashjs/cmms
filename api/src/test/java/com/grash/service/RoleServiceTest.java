package com.grash.service;

import com.grash.dto.RolePatchDTO;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.exception.CustomException;
import com.grash.mapper.RoleMapper;
import com.grash.model.Company;
import com.grash.model.CompanySettings;
import com.grash.model.Role;
import com.grash.model.Subscription;
import com.grash.model.SubscriptionPlan;
import com.grash.model.User;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PlanFeatures;
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
    @org.mockito.Spy
    private RoleMapper roleMapper = org.mapstruct.factory.Mappers.getMapper(RoleMapper.class);
    @Mock private com.grash.repository.UserRepository users;
    @Mock private com.grash.repository.CompanyRepository companies;
    @Mock private com.grash.repository.UserInvitationRepository invitations;
    @Mock private com.grash.repository.RefreshTokenRepository tokens;
    @Mock private CacheService cache;
    @Mock private jakarta.persistence.EntityManager em;
    @Mock
    private LicenseService licenseService;

    private Role adminRole;
    private Role customRole;
    private Company company;
    private CompanySettings otherCompanySettings;
    private User clientUser;
    private User restrictedUser;
    private User nonClientUser;
    private Role companyRole;
    private Role otherCompanyRole;

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

        company = new Company();
        company.setId(1L);
        company.getCompanySettings().setId(10L);
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setFeatures(new HashSet<>(Set.of(PlanFeatures.ROLE)));
        Subscription subscription = new Subscription();
        subscription.setSubscriptionPlan(plan);
        company.setSubscription(subscription);

        Role clientRole = Role.builder()
                .id(5L)
                .roleType(RoleType.ROLE_CLIENT)
                .name("Client Role")
                .viewPermissions(new HashSet<>(Collections.singleton(PermissionEntity.SETTINGS)))
                .createPermissions(new HashSet<>(Collections.singleton(PermissionEntity.WORK_ORDERS)))
                .viewOtherPermissions(new HashSet<>(Collections.singleton(PermissionEntity.WORK_ORDERS)))
                .editOtherPermissions(new HashSet<>(Collections.singleton(PermissionEntity.WORK_ORDERS)))
                .deleteOtherPermissions(new HashSet<>(Collections.singleton(PermissionEntity.WORK_ORDERS)))
                .build();

        clientUser = new User();
        clientUser.setId(10L);
        clientUser.setRole(clientRole);
        clientUser.setCompany(company);
        clientUser.setEnabled(true);

        Role restrictedRole = Role.builder()
                .id(6L)
                .roleType(RoleType.ROLE_CLIENT)
                .name("Restricted")
                .viewPermissions(new HashSet<>())
                .build();

        restrictedUser = new User();
        restrictedUser.setId(11L);
        restrictedUser.setRole(restrictedRole);
        restrictedUser.setCompany(company);
        restrictedUser.setEnabled(true);

        Role nonClientRole = Role.builder()
                .id(7L)
                .roleType(RoleType.ROLE_SUPER_ADMIN)
                .name("Super Admin")
                .build();

        nonClientUser = new User();
        nonClientUser.setId(12L);
        nonClientUser.setRole(nonClientRole);
        nonClientUser.setEnabled(true);

        otherCompanySettings = new CompanySettings();
        otherCompanySettings.setId(99L);

        companyRole = Role.builder()
                .id(4L)
                .roleType(RoleType.ROLE_CLIENT)
                .code(RoleCode.USER_CREATED)
                .name("Company Role")
                .companySettings(company.getCompanySettings())
                .build();

        otherCompanyRole = Role.builder()
                .id(4L)
                .roleType(RoleType.ROLE_CLIENT)
                .code(RoleCode.USER_CREATED)
                .name("Other Company Role")
                .companySettings(otherCompanySettings)
                .build();
        clientRole.setCode(RoleCode.USER_CREATED); clientRole.setCompanySettings(company.getCompanySettings());
        clientRole.getCreatePermissions().add(PermissionEntity.SETTINGS);
        clientRole.getEditOtherPermissions().add(PermissionEntity.SETTINGS);
        clientRole.getDeleteOtherPermissions().add(PermissionEntity.SETTINGS);
        restrictedRole.setCode(RoleCode.USER_CREATED); restrictedRole.setCompanySettings(company.getCompanySettings());
        CompanyAuthorizationService policy = new CompanyAuthorizationService(users, roleRepository, companies, invitations, tokens, cache, em);
        org.springframework.test.util.ReflectionTestUtils.setField(roleService, "authorization", policy);
        lenient().when(users.findById(10L)).thenReturn(Optional.of(clientUser));
        lenient().when(users.findById(11L)).thenReturn(Optional.of(restrictedUser));
        lenient().when(companies.lockAuthorization(1L)).thenReturn(Optional.of(company));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        com.grash.security.CustomUserDetail.builder().user(clientUser).build(), "", List.of()));

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

    @Nested
    class GetAllByUser {

        @Test
        void clientWithSettings_filtersByCompany() {
            when(roleRepository.findDefaultRoles()).thenReturn(new ArrayList<>());
            when(roleRepository.findByCompany_Id(1L)).thenReturn(List.of(companyRole));

            Collection<Role> result = roleService.getAll(clientUser);

            assertEquals(1, result.size());
            assertTrue(result.contains(companyRole));
            verify(roleRepository).findDefaultRoles();
            verify(roleRepository).findByCompany_Id(1L);
        }

        @Test
        void clientWithoutSettings_throwsForbidden() {
            CustomException ex = assertThrows(CustomException.class, () -> roleService.getAll(restrictedUser));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
            verify(roleRepository, never()).findAll();
        }

        @Test
        void nonClient_returnsAll() {
            when(roleRepository.findAll()).thenReturn(Arrays.asList(adminRole, customRole));

            Collection<Role> result = roleService.getAll(nonClientUser);

            assertEquals(2, result.size());
            verify(roleRepository).findAll();
        }
    }

    @Nested
    class GetById {

        @Test
        void existingAndBelongsToCompany_returnsRole() {
            when(roleRepository.findById(4L)).thenReturn(Optional.of(companyRole));

            Role result = roleService.getById(4L, clientUser);

            assertEquals(companyRole, result);
        }

        @Test
        void defaultRole_belongsToAllCompanies() {
            Role defaultTechnician = Role.builder()
                    .id(5L)
                    .roleType(RoleType.ROLE_CLIENT)
                    .code(RoleCode.TECHNICIAN)
                    .name("Technician")
                    .build();
            when(roleRepository.findById(5L)).thenReturn(Optional.of(defaultTechnician));

            Role result = roleService.getById(5L, clientUser);

            assertEquals(defaultTechnician, result);
        }

        @Test
        void clientWithoutSettings_throwsAccessDenied() {
            when(roleRepository.findById(4L)).thenReturn(Optional.of(companyRole));

            CustomException ex = assertThrows(CustomException.class, () -> roleService.getById(4L, restrictedUser));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void existingButNotBelongsToCompany_throwsAccessDenied() {
            when(roleRepository.findById(4L)).thenReturn(Optional.of(otherCompanyRole));

            CustomException ex = assertThrows(CustomException.class, () -> roleService.getById(4L, clientUser));

            assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        }

        @Test
        void nonExisting_throwsNotFound() {
            when(roleRepository.findById(99L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class, () -> roleService.getById(99L, clientUser));

            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }
    }






    @org.junit.jupiter.api.AfterEach void clearAuthentication() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Nested class AuthorizedMutations {
        @Test void createsScopedCustomRoleWithLicenseAndOwnedPermissions() {
            customRole.setId(null);
            when(licenseService.hasEntitlement(LicenseEntitlement.CUSTOM_ROLES)).thenReturn(true);
            when(roleRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            Role created = roleService.create(customRole, clientUser);
            assertSame(company.getCompanySettings(), created.getCompanySettings());
            assertEquals(RoleCode.USER_CREATED, created.getCode());
            assertEquals(RoleType.ROLE_CLIENT, created.getRoleType()); assertTrue(created.isPaid());
        }
        @Test void rawOverloadStillRequiresAuthorizationAndLicense() {
            customRole.setId(null);
            assertThrows(CustomException.class, () -> roleService.create(customRole));
            verify(roleRepository, never()).save(any());
        }
        @Test void rejectsExistingIdPlatformRoleMissingOperationOrSubscription() {
            assertThrows(CustomException.class, () -> roleService.create(customRole, clientUser));
            customRole.setId(null); customRole.setRoleType(RoleType.ROLE_SUPER_ADMIN);
            assertThrows(CustomException.class, () -> roleService.create(customRole, clientUser));
            customRole.setRoleType(RoleType.ROLE_CLIENT);
            assertThrows(CustomException.class, () -> roleService.create(customRole, restrictedUser));
            company.getSubscription().getSubscriptionPlan().setFeatures(Set.of());
            assertThrows(CustomException.class, () -> roleService.create(customRole, clientUser));
            verify(roleRepository, never()).save(any());
        }
        @Test void rejectsUnownedPermissionOnCreate() {
            customRole.setId(null); customRole.setViewPermissions(Set.of(PermissionEntity.ASSETS));
            when(licenseService.hasEntitlement(LicenseEntitlement.CUSTOM_ROLES)).thenReturn(true);
            assertThrows(CustomException.class, () -> roleService.create(customRole, clientUser));
            verify(roleRepository, never()).save(any());
        }
        @Test void partialPatchPreservesUnspecifiedPermissions() {
            companyRole.setCreatePermissions(new HashSet<>(Set.of(PermissionEntity.WORK_ORDERS)));
            when(roleRepository.findById(4L)).thenReturn(Optional.of(companyRole));
            when(roleRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            RolePatchDTO patch = new RolePatchDTO(); patch.setName("Updated");
            Role updated = roleService.update(4L, patch);
            assertEquals("Updated", updated.getName());
            assertEquals(Set.of(PermissionEntity.WORK_ORDERS), updated.getCreatePermissions());
            verifyNoInteractions(invitations, tokens);
        }
        @Test void rejectedPatchDoesNotMutateManagedEntity() {
            when(roleRepository.findById(4L)).thenReturn(Optional.of(companyRole));
            RolePatchDTO patch = new RolePatchDTO(); patch.setName("Should not persist");
            patch.setViewPermissions(List.of(PermissionEntity.ASSETS));
            assertThrows(CustomException.class, () -> roleService.patch(4L, patch, clientUser));
            assertEquals("Company Role", companyRole.getName()); assertTrue(companyRole.getViewPermissions().isEmpty());
            verify(roleRepository, never()).save(any());
        }
        @Test void rejectsForeignAndBuiltInRoleChanges() {
            when(roleRepository.findById(4L)).thenReturn(Optional.of(otherCompanyRole));
            assertThrows(CustomException.class, () -> roleService.patch(4L, new RolePatchDTO(), clientUser));
            assertThrows(CustomException.class, () -> roleService.deleteByIdAndUser(4L, clientUser));
            when(roleRepository.findById(1L)).thenReturn(Optional.of(adminRole));
            assertThrows(CustomException.class, () -> roleService.patch(1L, new RolePatchDTO(), clientUser));
            verify(roleRepository, never()).save(any()); verify(roleRepository, never()).delete(any(Role.class));
        }
        @Test void deletesAuthorizedUnassignedCustomRoleAndInvalidatesInvitations() {
            when(roleRepository.findById(4L)).thenReturn(Optional.of(companyRole));
            roleService.delete(4L);
            verify(roleRepository).delete(companyRole);
            verify(invitations).revokeByRole(eq(4L), any(Date.class));
        }
        @Test void rejectsUnknownRole() {
            assertThrows(CustomException.class, () -> roleService.update(99L, new RolePatchDTO()));
            verify(roleRepository, never()).save(any());
        }
    }

}
