package com.grash.integration;

import com.grash.advancedsearch.FilterField;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.UserPatchDTO;
import com.grash.model.*;
import com.grash.model.enums.RoleCode;
import com.grash.model.enums.RoleType;
import com.grash.repository.*;
import com.grash.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.grash.utils.Helper.setCurrentUser;
import static org.junit.jupiter.api.Assertions.*;

class UserServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private RoleRepository roleRepository;
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
    private LocationRepository locationRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Company company;
    private User user;
    private Role adminRole;
    private Role technicianRole;
    private Location location;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        SubscriptionPlan plan = SubscriptionPlan.builder()
                .name("Test Plan")
                .monthlyCostPerUser(10.0)
                .yearlyCostPerUser(100.0)
                .features(new java.util.HashSet<>())
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
        gp.setDateFormat(com.grash.model.enums.DateFormat.MMDDYY);
        gp.setTimeZone("UTC");
        generalPreferencesRepository.save(gp);

        adminRole = Role.builder()
                .name("Admin")
                .roleType(RoleType.ROLE_CLIENT)
                .code(RoleCode.ADMIN)
                .companySettings(settings)
                .paid(true)
                .build();
        adminRole = roleRepository.save(adminRole);

        technicianRole = Role.builder()
                .name("Technician")
                .roleType(RoleType.ROLE_CLIENT)
                .code(RoleCode.TECHNICIAN)
                .companySettings(settings)
                .paid(false)
                .build();
        technicianRole = roleRepository.save(technicianRole);

        user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@test.com");
        user.setUsername("johndoe");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(adminRole);
        user.setCompany(company);
        user.setEnabled(true);
        user.setEnabledInSubscription(true);
        user.setPhone("123-456-7890");
        user = userRepository.save(user);

        User secondUser = new User();
        secondUser.setFirstName("Jane");
        secondUser.setLastName("Smith");
        secondUser.setEmail("jane@test.com");
        secondUser.setUsername("janesmith");
        secondUser.setPassword(passwordEncoder.encode("password456"));
        secondUser.setRole(technicianRole);
        secondUser.setCompany(company);
        secondUser.setEnabled(true);
        secondUser.setEnabledInSubscription(true);
        secondUser = userRepository.save(secondUser);

        location = new Location();
        location.setName("Main Site");
        location.setCompany(company);
        location = locationRepository.save(location);

        user.setLocation(location);
        userRepository.save(user);
        secondUser.setLocation(location);
        userRepository.save(secondUser);

        setCurrentUser(user);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
        locationRepository.deleteAll();
        roleRepository.deleteAll();
        generalPreferencesRepository.deleteAll();
        companySettingsRepository.deleteAll();
        companyRepository.deleteAll();
        subscriptionRepository.deleteAll();
        subscriptionPlanRepository.deleteAll();
        SecurityContextHolder.clearContext();
    }

    // ===== FindTests =====

    @Test
    void findByEmail_returnsUser() {
        Optional<User> found = userService.findByEmail("john@test.com");

        assertTrue(found.isPresent());
        assertEquals("John", found.get().getFirstName());
    }

    @Test
    void findByEmail_caseInsensitive_returnsUser() {
        Optional<User> found = userService.findByEmail("JOHN@TEST.COM");

        assertTrue(found.isPresent());
        assertEquals("johndoe", found.get().getUsername());
    }

    @Test
    void findByEmail_nonExistent_returnsEmpty() {
        Optional<User> found = userService.findByEmail("nonexistent@test.com");

        assertTrue(found.isEmpty());
    }

    @Test
    void findByEmailAndCompany_returnsUser() {
        Optional<User> found = userService.findByEmailAndCompany("john@test.com", company.getId());

        assertTrue(found.isPresent());
        assertEquals("johndoe", found.get().getUsername());
    }

    @Test
    void findByEmailAndCompany_wrongCompany_returnsEmpty() {
        Optional<User> found = userService.findByEmailAndCompany("john@test.com", 999L);

        assertTrue(found.isEmpty());
    }

    @Test
    void findByIdAndCompany_returnsUser() {
        Optional<User> found = userService.findByIdAndCompany(user.getId(), company.getId());

        assertTrue(found.isPresent());
        assertEquals("john@test.com", found.get().getEmail());
    }

    @Test
    void findByIdAndCompany_wrongCompany_returnsEmpty() {
        Optional<User> found = userService.findByIdAndCompany(user.getId(), 999L);

        assertTrue(found.isEmpty());
    }

    @Test
    void findByCompany_returnsAllUsers() {
        Collection<User> users = userService.findByCompany(company.getId());

        assertEquals(2, users.size());
    }

    @Test
    void findByCompany_emptyCompany_returnsEmpty() {
        Subscription sub = subscriptionRepository.save(
                Subscription.builder().usersCount(5).subscriptionPlan(
                        subscriptionPlanRepository.findAll().get(0)).build());
        Company emptyCompany = new Company("Empty", 0, sub);
        CompanySettings settings = emptyCompany.getCompanySettings();
        GeneralPreferences gp = settings.getGeneralPreferences();
        gp.setCurrency(currencyRepository.findFirstBy().get());
        gp.setDateFormat(com.grash.model.enums.DateFormat.MMDDYY);
        gp.setTimeZone("UTC");
        emptyCompany = companyRepository.save(emptyCompany);

        Collection<User> users = userService.findByCompany(emptyCompany.getId());

        assertTrue(users.isEmpty());
    }

    @Test
    void findWorkersByCompany_excludesRequestersAndViewOnly() {
        Role requesterRole = Role.builder()
                .name("Requester")
                .roleType(RoleType.ROLE_CLIENT)
                .code(RoleCode.REQUESTER)
                .build();
        requesterRole = roleRepository.save(requesterRole);

        User requester = new User();
        requester.setFirstName("Req");
        requester.setLastName("Uester");
        requester.setEmail("requester@test.com");
        requester.setUsername("requester");
        requester.setPassword("encoded");
        requester.setRole(requesterRole);
        requester.setCompany(company);
        requester.setEnabled(true);
        requester.setEnabledInSubscription(true);
        userRepository.save(requester);

        Collection<User> workers = userService.findWorkersByCompany(company.getId());

        assertEquals(2, workers.size());
        assertTrue(workers.stream().noneMatch(w -> w.getEmail().equals("requester@test.com")));
    }

    @Test
    void findByLocation_returnsUsersAtLocation() {
        Collection<User> users = userService.findByLocation(location.getId());

        assertEquals(2, users.size());
    }

    @Test
    void findByLocation_emptyLocation_returnsEmpty() {
        Location otherLocation = new Location();
        otherLocation.setName("Other Site");
        otherLocation.setCompany(company);
        otherLocation = locationRepository.save(otherLocation);

        Collection<User> users = userService.findByLocation(otherLocation.getId());

        assertTrue(users.isEmpty());
    }

    @Test
    void existsByEmail_returnsTrue() {
        assertTrue(userRepository.existsByEmailIgnoreCase("john@test.com"));
    }

    @Test
    void existsByEmail_nonExistent_returnsFalse() {
        assertFalse(userRepository.existsByEmailIgnoreCase("notfound@test.com"));
    }

    @Test
    void getAll_returnsAllUsers() {
        assertEquals(2, userService.getAll().size());
    }

    @Test
    void count_returnsCorrectCount() {
        assertEquals(2, userService.count());
    }

    @Test
    void findById_returnsUser() {
        Optional<User> found = userService.findById(user.getId());

        assertTrue(found.isPresent());
        assertEquals("john@test.com", found.get().getEmail());
    }

    // ===== SaveTests =====

    @Test
    void save_persistsUser() {
        User newUser = new User();
        newUser.setFirstName("New");
        newUser.setLastName("User");
        newUser.setEmail("newuser@test.com");
        newUser.setUsername("newuser");
        newUser.setPassword("encoded");
        newUser.setRole(adminRole);
        newUser.setCompany(company);
        newUser.setEnabled(true);

        User saved = userService.save(newUser);

        assertNotNull(saved.getId());

        Optional<User> found = userRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("newuser@test.com", found.get().getEmail());
    }

    @Test
    void saveAll_persistsMultipleUsers() {
        User u1 = new User();
        u1.setFirstName("A");
        u1.setLastName("One");
        u1.setEmail("aone@test.com");
        u1.setUsername("aone");
        u1.setPassword("encoded");
        u1.setRole(adminRole);
        u1.setCompany(company);
        u1.setEnabled(true);

        User u2 = new User();
        u2.setFirstName("B");
        u2.setLastName("Two");
        u2.setEmail("btwo@test.com");
        u2.setUsername("btwo");
        u2.setPassword("encoded");
        u2.setRole(technicianRole);
        u2.setCompany(company);
        u2.setEnabled(true);

        Collection<User> saved = userService.saveAll(List.of(u1, u2));

        assertEquals(2, saved.size());
        assertEquals(4, userRepository.count());
    }

    // ===== UpdateTests =====

    @Test
    void update_changesUserFields() {
        UserPatchDTO patch = new UserPatchDTO();
        patch.setFirstName("Jonathan");
        patch.setLastName("Doe");
        patch.setPhone("999-888-7777");

        User updated = userService.update(user.getId(), patch);

        assertEquals("Jonathan", updated.getFirstName());
        assertEquals("999-888-7777", updated.getPhone());
    }

    @Test
    void update_withPassword_changesPassword() {
        UserPatchDTO patch = new UserPatchDTO();
        patch.setFirstName("John");
        patch.setLastName("Doe");
        patch.setNewPassword("newpassword123");

        User updated = userService.update(user.getId(), patch);

        assertNotNull(updated);
        assertTrue(passwordEncoder.matches("newpassword123", updated.getPassword()));
    }

    @Test
    void update_nonExistent_throwsException() {
        UserPatchDTO patch = new UserPatchDTO();
        patch.setFirstName("Ghost");

        assertThrows(Exception.class, () -> userService.update(999L, patch));
    }

    // ===== DeleteTests =====

    @Test
    void deleteByUsername_removesUser() {
        assertTrue(userRepository.findByEmailIgnoreCase("jane@test.com").isPresent());

        userService.delete("janesmith");


        assertTrue(userRepository.findByEmailIgnoreCase("jane@test.com").isEmpty());
    }

    @Test
    void deleteByUsername_nonExistent_doesNothing() {
        assertDoesNotThrow(() -> userService.delete("nonexistent"));
    }

    // ===== EnableUserTests =====

    @Test
    void enableUser_self_enablesUser() {
        User disabled = new User();
        disabled.setFirstName("Disabled");
        disabled.setLastName("User");
        disabled.setEmail("disabled@test.com");
        disabled.setUsername("disabled");
        disabled.setPassword("encoded");
        disabled.setRole(technicianRole);
        disabled.setCompany(company);
        disabled.setEnabled(false);
        disabled.setEnabledInSubscription(false);
        disabled = userRepository.save(disabled);


        User enabled = userService.enableUser(disabled);

        assertTrue(enabled.isEnabled());
        assertTrue(enabled.isEnabledInSubscription());
    }

    @Test
    void enableUser_byEmail_enablesUser() {
        User disabled = new User();
        disabled.setFirstName("Disabled2");
        disabled.setLastName("User2");
        disabled.setEmail("disabled2@test.com");
        disabled.setUsername("disabled2");
        disabled.setPassword("encoded");
        disabled.setRole(technicianRole);
        disabled.setCompany(company);
        disabled.setEnabled(false);
        disabled.setEnabledInSubscription(false);
        disabled = userRepository.save(disabled);


        assertDoesNotThrow(() -> userService.enableUser("disabled2@test.com"));


        Optional<User> reloaded = userRepository.findByEmailIgnoreCase("disabled2@test.com");
        assertTrue(reloaded.get().isEnabled());
        assertTrue(reloaded.get().isEnabledInSubscription());
    }

    // ===== SearchCriteriaTests =====

    @Test
    void findBySearchCriteria_filtersByEmail() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setPageNum(0);
        criteria.setPageSize(10);
        criteria.setSortField("id");
        criteria.setDirection(org.springframework.data.domain.Sort.Direction.ASC);
        criteria.setFilterFields(List.of(
                FilterField.builder().field("email").value("john@test.com").operation("eq").build()
        ));

        Page<User> result = userService.findBySearchCriteria(criteria);

        assertEquals(1, result.getTotalElements());
        assertEquals("john@test.com", result.getContent().get(0).getEmail());
    }

    @Test
    void findBySearchCriteria_filtersByCompanyId() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setPageNum(0);
        criteria.setPageSize(10);
        criteria.setSortField("id");
        criteria.setDirection(org.springframework.data.domain.Sort.Direction.ASC);
        criteria.setFilterFields(List.of(
                FilterField.builder().field("company.id").value(String.valueOf(company.getId())).operation("eq").build()
        ));

        Page<User> result = userService.findBySearchCriteria(criteria);

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findBySearchCriteria_pagination() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setPageNum(0);
        criteria.setPageSize(1);
        criteria.setSortField("id");
        criteria.setDirection(org.springframework.data.domain.Sort.Direction.ASC);
        criteria.setFilterFields(List.of(
                FilterField.builder().field("company.id").value(String.valueOf(company.getId())).operation("eq").build()
        ));

        Page<User> page1 = userService.findBySearchCriteria(criteria);

        assertEquals(1, page1.getContent().size());
        assertEquals(2, page1.getTotalElements());

        criteria.setPageNum(1);
        Page<User> page2 = userService.findBySearchCriteria(criteria);

        assertEquals(1, page2.getContent().size());
    }

    // ===== PaidUsersQueryTests =====

    @Test
    void hasMorePaidUsersThan_returnsTrue_whenExceeded() {
        user.setEnabled(true);
        user.setEnabledInSubscription(true);
        userRepository.save(user);


        boolean exceeded = userRepository.hasMorePaidUsersThan(0);

        assertTrue(exceeded);
    }

    @Test
    void hasMorePaidUsersThan_returnsFalse_whenWithinLimit() {
        boolean exceeded = userRepository.hasMorePaidUsersThan(5);

        assertFalse(exceeded);
    }

    // ===== SSOQueryTests =====

    @Test
    void findBySSOCompany_returnsMatchingUsers() {
        User ssoUser = new User();
        ssoUser.setFirstName("SSO");
        ssoUser.setLastName("User");
        ssoUser.setEmail("sso@mycorp.com");
        ssoUser.setUsername("ssouser");
        ssoUser.setPassword("encoded");
        ssoUser.setRole(technicianRole);
        ssoUser.setCompany(company);
        ssoUser.setEnabled(true);
        ssoUser.setCreatedViaSso(true);
        userRepository.save(ssoUser);


        List<User> ssoUsers = userRepository.findBySSOCompany("mycorp.com");

        assertEquals(1, ssoUsers.size());
        assertEquals("ssouser", ssoUsers.get(0).getUsername());
    }

    @Test
    void findBySsoProviderIdAndSsoProvider_returnsUser() {
        User ssoUser = new User();
        ssoUser.setFirstName("LDAP");
        ssoUser.setLastName("User");
        ssoUser.setEmail("ldap@test.com");
        ssoUser.setUsername("ldapuser");
        ssoUser.setPassword("encoded");
        ssoUser.setRole(technicianRole);
        ssoUser.setCompany(company);
        ssoUser.setEnabled(true);
        ssoUser.setSsoProviderId("ldap-123");
        ssoUser.setSsoProvider("azure-ad");
        userRepository.save(ssoUser);


        Optional<User> found = userRepository.findBySsoProviderIdAndSsoProvider("ldap-123", "azure-ad");

        assertTrue(found.isPresent());
        assertEquals("ldapuser", found.get().getUsername());
    }

    @Test
    void findBySsoProviderIdAndSsoProvider_wrongProvider_returnsEmpty() {
        Optional<User> found = userRepository.findBySsoProviderIdAndSsoProvider("nonexistent", "azure-ad");

        assertTrue(found.isEmpty());
    }
}
