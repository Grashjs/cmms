package com.grash.integration;

import com.grash.dto.RolePatchDTO;
import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.enums.DateFormat;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleCode;
import com.grash.model.enums.RoleType;
import com.grash.repository.*;
import com.grash.service.RoleService;
import com.grash.utils.Helper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class RoleIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private CompanySettingsRepository companySettingsRepository;
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private SubscriptionPlanRepository subscriptionPlanRepository;
    @Autowired
    private CurrencyRepository currencyRepository;
    @Autowired
    private GeneralPreferencesRepository generalPreferencesRepository;
    @Autowired
    private RoleService roleService;
    @Autowired
    private EntityManager em;

    @Autowired private UserRepository userRepository;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean private com.grash.service.LicenseService license;
    private Company company;
    private Role companyRole;

    @BeforeEach
    void setUp() {
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .name("Test Plan")
                .monthlyCostPerUser(10.0)
                .yearlyCostPerUser(100.0)
                .features(new HashSet<>(Set.of(com.grash.model.enums.PlanFeatures.ROLE)))
                .build();
        plan = subscriptionPlanRepository.save(plan);

        Subscription subscription = Subscription.builder()
                .usersCount(10)
                .subscriptionPlan(plan)
                .build();
        subscription = subscriptionRepository.save(subscription);

        company = new Company("TestCompany", 10, subscription);
        company = companyRepository.save(company);

        CompanySettings settings = company.getCompanySettings();
        settings.setCompany(company);
        companySettingsRepository.save(settings);

        GeneralPreferences gp = settings.getGeneralPreferences();
        gp.setCurrency(currencyRepository.findFirstBy().get());
        gp.setDateFormat(DateFormat.MMDDYY);
        gp.setTimeZone("UTC");
        generalPreferencesRepository.save(gp);

        companyRole = Role.builder()
                .name("Custom Role")
                .roleType(RoleType.ROLE_CLIENT)
                .code(RoleCode.USER_CREATED)
                .companySettings(settings)
                .paid(true)
                .build();
        companyRole = roleRepository.save(companyRole);
        User owner = new User(); owner.setCompany(company); owner.setRole(roleRepository.findDefaultRoleWithCode(RoleCode.ADMIN).orElseThrow());
        owner.setEmail("role-test-" + UUID.randomUUID() + "@example.com"); owner.setUsername(UUID.randomUUID().toString());
        owner.setFirstName("Role"); owner.setLastName("Owner"); owner.setPassword("fixture"); owner.setEnabled(true); owner.setOwnsCompany(true);
        owner = userRepository.saveAndFlush(owner);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        com.grash.security.CustomUserDetail.builder().user(owner).build(), "", List.of()));
    }

    @org.junit.jupiter.api.AfterEach void resetLicense() { org.mockito.Mockito.reset(license); }

    // ===== FindTests =====

    @Test
    void findById_returnsRole() {
        Optional<Role> found = roleService.findById(companyRole.getId());

        assertTrue(found.isPresent());
        assertEquals("Custom Role", found.get().getName());
    }

    @Test
    void findById_nonExistent_returnsEmpty() {
        Optional<Role> found = roleService.findById(999L);

        assertTrue(found.isEmpty());
    }

    @Test
    void findByCompany_returnsDefaultPlusCompanyRoles() {
        List<Role> result = roleService.findByCompany(company.getId());

        assertEquals(7, result.size());
        assertTrue(result.contains(companyRole));
        Set<RoleCode> codes = result.stream().map(Role::getCode).collect(Collectors.toSet());
        assertTrue(codes.contains(RoleCode.ADMIN));
        assertTrue(codes.contains(RoleCode.TECHNICIAN));
        assertTrue(codes.contains(RoleCode.VIEW_ONLY));
    }

    @Test
    void findByCompany_doesNotIncludeRolesOfOtherCompanies() {
        Subscription sub = subscriptionRepository.save(
                Subscription.builder().usersCount(5).subscriptionPlan(
                        subscriptionPlanRepository.findAll().get(0)).build());
        Company otherCompany = new Company("OtherCompany", 5, sub);
        CompanySettings otherSettings = otherCompany.getCompanySettings();
        GeneralPreferences gp = otherSettings.getGeneralPreferences();
        gp.setCurrency(currencyRepository.findFirstBy().get());
        gp.setDateFormat(DateFormat.MMDDYY);
        gp.setTimeZone("UTC");
        otherCompany = companyRepository.save(otherCompany);

        Role otherRole = Role.builder()
                .name("Other Company Role")
                .roleType(RoleType.ROLE_CLIENT)
                .code(RoleCode.USER_CREATED)
                .companySettings(otherSettings)
                .paid(true)
                .build();
        otherRole = roleRepository.save(otherRole);

        List<Role> result = roleService.findByCompany(company.getId());

        assertEquals(7, result.size());
        assertFalse(result.contains(otherRole));
    }

    @Test
    void findDefaultRoles_returnsOnlyDefaultRoles() {
        List<Role> result = roleService.findDefaultRoles();

        assertEquals(6, result.size());
        assertTrue(result.stream().allMatch(role -> role.getCompanySettings() == null));
        assertTrue(result.stream().noneMatch(role -> role.getCode() == RoleCode.USER_CREATED));
        Set<RoleCode> codes = result.stream().map(Role::getCode).collect(Collectors.toSet());
        assertTrue(codes.contains(RoleCode.ADMIN));
        assertTrue(codes.contains(RoleCode.LIMITED_ADMIN));
        assertTrue(codes.contains(RoleCode.TECHNICIAN));
        assertTrue(codes.contains(RoleCode.LIMITED_TECHNICIAN));
        assertTrue(codes.contains(RoleCode.VIEW_ONLY));
        assertTrue(codes.contains(RoleCode.REQUESTER));
    }

    @Test
    void findDefaultRoleWithCode_returnsDefaultRole() {
        Optional<Role> found = roleService.findDefaultRoleWithCode(RoleCode.ADMIN);

        assertTrue(found.isPresent());
        assertEquals(RoleCode.ADMIN, found.get().getCode());
        assertNull(found.get().getCompanySettings());
    }

    @Test
    void findDefaultRoleWithCode_userCreatedCode_returnsEmpty() {
        Optional<Role> found = roleService.findDefaultRoleWithCode(RoleCode.USER_CREATED);

        assertTrue(found.isEmpty());
    }

    @Test
    void getAll_returnsAllRolesIncludingDefaults() {
        Collection<Role> result = roleService.getAll();

        assertTrue(result.size() >= 7);
        assertTrue(result.contains(companyRole));
        Set<RoleCode> codes = result.stream().map(Role::getCode).collect(Collectors.toSet());
        assertTrue(codes.contains(RoleCode.ADMIN));
        assertTrue(codes.contains(RoleCode.TECHNICIAN));
    }

    // ===== CreateTests =====

    @Test
    void create_requestedDefaultCodeIsStoredAsScopedCustomRole() {
        org.mockito.Mockito.doReturn(true).when(license).hasEntitlement(com.grash.dto.license.LicenseEntitlement.CUSTOM_ROLES);
        Role role = Role.builder()
                .name("New Admin")
                .roleType(RoleType.ROLE_CLIENT)
                .code(RoleCode.ADMIN)
                .paid(true)
                .createPermissions(new HashSet<>(List.of(PermissionEntity.WORK_ORDERS, PermissionEntity.ASSETS)))
                .viewPermissions(new HashSet<>(List.of(PermissionEntity.SETTINGS)))
                .build();

        Role saved = roleService.create(role);

        assertNotNull(saved.getId());

        Optional<Role> reloaded = roleRepository.findById(saved.getId());
        assertTrue(reloaded.isPresent());
        assertEquals(RoleCode.USER_CREATED, reloaded.get().getCode());
        assertEquals(company.getCompanySettings().getId(), reloaded.get().getCompanySettings().getId());
        assertEquals(new HashSet<>(List.of(PermissionEntity.WORK_ORDERS, PermissionEntity.ASSETS)),
                reloaded.get().getCreatePermissions());
        assertEquals(new HashSet<>(List.of(PermissionEntity.SETTINGS)), reloaded.get().getViewPermissions());
    }

    @Test
    void create_userCreatedRole_withoutLicense_throwsForbidden() {
        org.mockito.Mockito.doReturn(false).when(license).hasEntitlement(com.grash.dto.license.LicenseEntitlement.CUSTOM_ROLES);
        Role customRole = Role.builder()
                .name("Custom")
                .roleType(RoleType.ROLE_CLIENT)
                .code(RoleCode.USER_CREATED)
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> roleService.create(customRole));

        assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        assertTrue(roleRepository.findByName("Custom").isEmpty());
    }

    // ===== UpdateTests =====

    @Test
    void update_changesRoleFields() {
        RolePatchDTO patch = new RolePatchDTO();
        patch.setName("Updated Role");
        patch.setDescription("Updated description");
        patch.setExternalId("ext-1");
        patch.setCreatePermissions(List.of(PermissionEntity.WORK_ORDERS, PermissionEntity.LOCATIONS));
        patch.setViewPermissions(List.of(PermissionEntity.SETTINGS));
        patch.setViewOtherPermissions(new ArrayList<>());
        patch.setEditOtherPermissions(new ArrayList<>());
        patch.setDeleteOtherPermissions(new ArrayList<>());

        Role updated = roleService.update(companyRole.getId(), patch);

        assertEquals("Updated Role", updated.getName());
        assertEquals("Updated description", updated.getDescription());
        assertEquals(new HashSet<>(List.of(PermissionEntity.WORK_ORDERS, PermissionEntity.LOCATIONS)),
                updated.getCreatePermissions());
        assertEquals(new HashSet<>(List.of(PermissionEntity.SETTINGS)), updated.getViewPermissions());

        Optional<Role> reloaded = roleRepository.findById(companyRole.getId());
        assertTrue(reloaded.isPresent());
        assertEquals("Updated Role", reloaded.get().getName());
        assertEquals(new HashSet<>(List.of(PermissionEntity.WORK_ORDERS, PermissionEntity.LOCATIONS)),
                reloaded.get().getCreatePermissions());
    }

    @Test
    void update_nonExistent_throwsNotFound() {
        RolePatchDTO patch = new RolePatchDTO();
        patch.setName("Ghost");
        patch.setCreatePermissions(new ArrayList<>());
        patch.setViewPermissions(new ArrayList<>());
        patch.setViewOtherPermissions(new ArrayList<>());
        patch.setEditOtherPermissions(new ArrayList<>());
        patch.setDeleteOtherPermissions(new ArrayList<>());

        CustomException ex = assertThrows(CustomException.class,
                () -> roleService.update(999L, patch));

        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
    }

    // ===== DeleteTests =====

    @Test
    void delete_removesRole() {
        Role roleToDelete = roleRepository.saveAndFlush(Role.builder()
                .name("To Delete")
                .roleType(RoleType.ROLE_CLIENT)
                .code(RoleCode.USER_CREATED)
                .companySettings(company.getCompanySettings())
                .paid(false)
                .build());

        roleService.delete(roleToDelete.getId());

        assertTrue(roleRepository.findById(roleToDelete.getId()).isEmpty());
        assertTrue(roleRepository.findByName("To Delete").isEmpty());
    }

    @Test
    void delete_nonExistent_isRejected() {
        assertThrows(CustomException.class, () -> roleService.delete(999L));
    }

    // ===== SaveAllTests =====

    @Test
    void saveAll_persistsAllRoles() {
        Role r1 = Role.builder()
                .name("Bulk 1")
                .roleType(RoleType.ROLE_CLIENT)
                .code(RoleCode.USER_CREATED)
                .companySettings(company.getCompanySettings())
                .paid(false)
                .build();
        Role r2 = Role.builder()
                .name("Bulk 2")
                .roleType(RoleType.ROLE_CLIENT)
                .code(RoleCode.USER_CREATED)
                .companySettings(company.getCompanySettings())
                .paid(false)
                .build();

        List<Role> saved = roleService.saveAll(List.of(r1, r2));

        assertEquals(2, saved.size());
        assertNotNull(saved.get(0).getId());
        assertNotNull(saved.get(1).getId());
        assertTrue(roleRepository.findByName("Bulk 1").isPresent());
        assertTrue(roleRepository.findByName("Bulk 2").isPresent());
    }

    // ===== UpdateDefaultRolesTests =====

    @Test
    void updateDefaultRoles_whenDefaultsAreCurrent_noChanges() {
        roleService.updateDefaultRoles();

        List<Role> defaults = roleService.findDefaultRoles();
        assertEquals(6, defaults.size());

        Role upToDateAdmin = Helper.getDefaultRoles().stream()
                .filter(role -> role.getCode() == RoleCode.ADMIN)
                .findFirst().orElseThrow();
        Role currentAdmin = defaults.stream()
                .filter(role -> role.getCode() == RoleCode.ADMIN)
                .findFirst().orElseThrow();
        assertEquals(upToDateAdmin.getCreatePermissions(), currentAdmin.getCreatePermissions());
        assertEquals(upToDateAdmin.isPaid(), currentAdmin.isPaid());
    }

    @Test
    void updateDefaultRoles_restoresStaleDefaultRole() {
        Role staleAdmin = roleService.findDefaultRoleWithCode(RoleCode.ADMIN).orElseThrow();
        staleAdmin.getCreatePermissions().clear();
        staleAdmin.setPaid(false);
        roleRepository.saveAndFlush(staleAdmin);
        em.clear();

        roleService.updateDefaultRoles();

        Role restoredAdmin = roleService.findDefaultRoleWithCode(RoleCode.ADMIN).orElseThrow();
        Role upToDateAdmin = Helper.getDefaultRoles().stream()
                .filter(role -> role.getCode() == RoleCode.ADMIN)
                .findFirst().orElseThrow();
        assertEquals(upToDateAdmin.getCreatePermissions(), restoredAdmin.getCreatePermissions());
        assertEquals(upToDateAdmin.getViewPermissions(), restoredAdmin.getViewPermissions());
        assertEquals(upToDateAdmin.getViewOtherPermissions(), restoredAdmin.getViewOtherPermissions());
        assertEquals(upToDateAdmin.getEditOtherPermissions(), restoredAdmin.getEditOtherPermissions());
        assertEquals(upToDateAdmin.getDeleteOtherPermissions(), restoredAdmin.getDeleteOtherPermissions());
        assertTrue(restoredAdmin.isPaid());
    }

    @Test
    void updateDefaultRoles_doesNotTouchCompanyRoles() {
        roleService.updateDefaultRoles();

        Optional<Role> reloaded = roleRepository.findById(companyRole.getId());
        assertTrue(reloaded.isPresent());
        assertEquals("Custom Role", reloaded.get().getName());
        assertEquals(companyRole.getCompanySettings().getId(),
                reloaded.get().getCompanySettings().getId());
    }
}
