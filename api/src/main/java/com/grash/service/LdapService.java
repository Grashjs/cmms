package com.grash.service;

import com.grash.dto.LdapLoginRequest;
import com.grash.exception.CustomException;
import com.grash.model.Company;
import com.grash.model.Role;
import com.grash.model.User;
import com.grash.model.enums.RoleCode;
import com.grash.repository.UserRepository;
import com.grash.security.JwtTokenProvider;
import com.grash.utils.Helper;
import com.grash.utils.Utils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.stereotype.Service;

import javax.naming.directory.SearchControls;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class LdapService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final Utils utils;
    private final RoleService roleService;
    private final CompanyService companyService;
    private final CacheService cacheService;

    @Autowired(required = false)
    private LdapAuthenticationProvider ldapAuthenticationProvider;

    @Autowired(required = false)
    private LdapTemplate ldapTemplate;

    @Value("${ldap.org-admin:}")
    private String ldapOrgAdmin;

    @Value("${LDAP_ATTR_EMAIL:mail}")
    private String ldapAttrEmail;

    @Value("${LDAP_ATTR_FIRSTNAME:givenName}")
    private String ldapAttrFirstName;

    @Value("${LDAP_ATTR_LASTNAME:sn}")
    private String ldapAttrLastName;

    @Value("${ldap.attributes.username:uid}")
    private String usernameAttr;

    @Value("${ldap.attributes.object-class:inetOrgPerson}")
    private String objectClassAttr;

    @Value("${ldap.user-search-bases:}")
    private String ldapUserSearchBases;

    @Value("${ldap.user-search-filter:}")
    private String ldapUserSearchFilter;

    @Value("${ldap.search-subtree:true}")
    private boolean ldapSearchSubtree;

    @Value("${ldap.sync.enabled:false}")
    private boolean ldapSyncEnabled;
    @Value("${ldap.sync.create:true}")
    private boolean ldapSyncCreate;
    @Value("${ldap.sync.update:true}")
    private boolean ldapSyncUpdate;
    @Value("${ldap.sync.disable:false}")
    private boolean ldapSyncDisable;

    @Value("${ldap.ou-role-mappings:}")
    private String ldapOuRoleMappings;

    @Autowired
    private UserService userService;


    @org.springframework.context.annotation.Profile("!test")
    public void syncLdapUsers() {
        if (!ldapSyncEnabled || ldapTemplate == null || ldapOrgAdmin == null || ldapOrgAdmin.isBlank()) {
            return;
        }

        Company company = companyService.findByOwnerEmailAndOwnsCompany(ldapOrgAdmin)
                .orElse(null);
        if (company == null) {
            return;
        }

        Set<String> ldapUsernames = new HashSet<>(fetchAllLdapUsernames());
        Map<String, User> existingLdapUsersByProviderId = new HashMap<>();
        Collection<User> existingUsers = userRepository.findByCompany_Id(company.getId());

        for (User user : existingUsers) {
            if (!"LDAP".equals(user.getSsoProvider()) || user.getSsoProviderId() == null) {
                continue;
            }
            existingLdapUsersByProviderId.put(user.getSsoProviderId(), user);

            if (!ldapUsernames.contains(user.getSsoProviderId())) {
                if (ldapSyncDisable) {
                    user.setEnabled(false);
                    userRepository.save(user);
                    cacheService.evictUserFromCache(user.getEmail());
                }
            }
        }

        for (String ldapUsername : ldapUsernames) {
            if (existingLdapUsersByProviderId.containsKey(ldapUsername)) {
                User user = existingLdapUsersByProviderId.get(ldapUsername);
                if (ldapSyncUpdate) {
                    Map<String, String> ldapDetails = getLdapUserDetailsByUsername(ldapUsername);
                    boolean updated = false;
                    if (ldapDetails.get("email") != null && !ldapDetails.get("email").equals(user.getEmail())) {
                        user.setEmail(ldapDetails.get("email"));
                        updated = true;
                    }
                    if (ldapDetails.get("firstName") != null && !ldapDetails.get("firstName").equals(user.getFirstName())) {
                        user.setFirstName(ldapDetails.get("firstName"));
                        updated = true;
                    }
                    if (ldapDetails.get("lastName") != null && !ldapDetails.get("lastName").equals(user.getLastName())) {
                        user.setLastName(ldapDetails.get("lastName"));
                        updated = true;
                    }
                    if (updated) {
                        userRepository.save(user);
                        cacheService.evictUserFromCache(user.getEmail());
                    }
                }
                if (!user.isEnabled() && ldapSyncDisable) {
                    user.setEnabled(true);
                    userRepository.save(user);
                }
            } else {
                if (ldapSyncCreate) {
                    Map<String, String> ldapDetails = getLdapUserDetailsByUsername(ldapUsername);
                    User newUser = getNewLdapUser(ldapUsername, company, ldapDetails);
                    userRepository.save(newUser);
                }
            }
        }
    }

    private User getNewLdapUser(String ldapUsername, Company company, Map<String, String> ldapDetails) {
        User user = new User();
        user.setUsername(utils.generateStringId());
        user.setEmail(ldapDetails.get("email"));
        user.setFirstName(ldapDetails.get("firstName"));
        user.setLastName(ldapDetails.get("lastName"));
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setEnabled(true);
        user.setCreatedViaSso(true);
        user.setSsoProvider("LDAP");
        user.setSsoProviderId(ldapUsername);
        user.setLastLogin(null);

        List<String> userOus = getLdapUserOu(ldapUsername);
        user.setRole(getRoleForOu(userOus));

        user.setCompany(company);
        return user;
    }

    private List<String> fetchAllLdapUsernames() {
        List<String> usernames = new ArrayList<>();
        try {
            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(ldapSearchSubtree ? SearchControls.SUBTREE_SCOPE :
                    SearchControls.ONELEVEL_SCOPE);
            AttributesMapper<String> mapper = attrs -> {
                try {
                    return attrs.get(usernameAttr) != null ? attrs.get(usernameAttr).get().toString() : null;
                } catch (Exception e) {
                    return null;
                }
            };
            List<String> bases = (ldapUserSearchBases != null && !ldapUserSearchBases.isBlank()
                    && ldapUserSearchFilter != null && !ldapUserSearchFilter.isBlank())
                    ? Arrays.asList(ldapUserSearchBases.split("[|]"))
                    : List.of("");
            String filter = (ldapUserSearchFilter != null && !ldapUserSearchFilter.isBlank())
                    ? ldapUserSearchFilter.replace("{0}", "*")
                    : new EqualsFilter("objectClass", objectClassAttr).encode();
            for (String base : bases) {
                String searchBase = base.trim();
                if (searchBase.isBlank()) searchBase = "";
                ldapTemplate.search(searchBase, filter, searchControls, mapper)
                        .stream()
                        .filter(Objects::nonNull)
                        .forEach(usernames::add);
            }
        } catch (Exception e) {
            // Return empty list on error
        }
        return usernames;
    }

    private Map<String, String> getLdapUserDetailsByUsername(String username) {
        return extractLdapUserDetails(username);
    }


    public String signinLdap(LdapLoginRequest ldapRequest) {
        try {
            if (ldapAuthenticationProvider == null) {
                throw new CustomException("LDAP authentication is not enabled", HttpStatus.FORBIDDEN);
            }
            if (ldapOrgAdmin == null || ldapOrgAdmin.isBlank()) {
                throw new CustomException("LDAP organization admin email is not configured",
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
            if (Helper.isValidEmailAddress(ldapRequest.getUsername())) {
                User user = userRepository.findByEmailIgnoreCase(ldapRequest.getUsername()).orElse(null);
                if (user != null && !"LDAP".equals(user.getSsoProvider())) {
                    return userService.signin(ldapRequest.getUsername(), ldapRequest.getPassword(), "CLIENT");
                }
            }
            String inputUsername = ldapRequest.getUsername();
            String ldapUsername = inputUsername;
            User user = null;

            if (inputUsername.contains("@")) {
                Optional<User> userByEmail = userRepository.findByEmailIgnoreCase(inputUsername);
                if (userByEmail.isPresent() && "LDAP".equals(userByEmail.get().getSsoProvider())) {
                    user = userByEmail.get();
                    ldapUsername = user.getSsoProviderId();
                }
            }

            if (user == null) {
                user = findUserByLdapId(inputUsername);
                if (user != null) {
                    ldapUsername = user.getSsoProviderId();
                }
            }


            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(ldapUsername, ldapRequest.getPassword());
            ldapAuthenticationProvider.authenticate(authToken);

            String userEmail = user != null ? user.getEmail() : ldapUsername;
            cacheService.evictUserFromCache(userEmail);

            if (user == null) {
                Map<String, String> ldapUserDetails = extractLdapUserDetails(ldapUsername);
                Company company = companyService.findByOwnerEmailAndOwnsCompany(ldapOrgAdmin)
                        .orElseThrow(() -> new CustomException("No company available for LDAP user",
                                HttpStatus.INTERNAL_SERVER_ERROR));
                user = getNewLdapUser(ldapUsername, company, ldapUserDetails);
                user.setLastLogin(new Date());

            } else {
                user.setLastLogin(new Date());
                List<String> userOus = getLdapUserOu(ldapUsername);
                user.setRole(getRoleForOu(userOus));
            }

            user = userRepository.save(user);
            cacheService.putUserInCache(user);
            return jwtTokenProvider.createToken(user.getEmail(),
                    Collections.singletonList(user.getRole().getRoleType()));
        } catch (AuthenticationException e) {
            throw new CustomException("LDAP authentication failed: " + e.getMessage(), HttpStatus.FORBIDDEN);
        }
    }

    private Role getDefaultRoleForLdapUser() {
        return roleService.findDefaultRoles().stream()
                .filter(role -> role.getCode().equals(RoleCode.LIMITED_TECHNICIAN))
                .findFirst()
                .orElseThrow(() -> new CustomException("Default role not found",
                        HttpStatus.INTERNAL_SERVER_ERROR));
    }

    private Map<String, String> extractLdapUserDetails(String username) {
        Map<String, String> userDetails = new HashMap<>();

        // ALWAYS derived from username (single source of truth)
        String safeUsername = (username == null || username.isBlank()) ? "unknown" : username;
        String safeEmail = safeUsername.contains("@") ? safeUsername : safeUsername + "@local";

        userDetails.put("email", safeEmail);
        userDetails.put("firstName", safeUsername);
        userDetails.put("lastName", "");
        userDetails.put("source", "fallback");

        try {
            if (ldapTemplate == null || usernameAttr == null || objectClassAttr == null) {
                return userDetails;
            }

            EqualsFilter filter = new EqualsFilter(usernameAttr, safeUsername);

            AndFilter andFilter = new AndFilter();
            andFilter.and(new EqualsFilter("objectClass", objectClassAttr));
            andFilter.and(filter);

            List<Map<String, String>> results = ldapTemplate.search(
                    "",
                    andFilter.encode(),
                    (AttributesMapper<Map<String, String>>) attrs -> {
                        Map<String, String> details = new HashMap<>();

                        try {
                            if (attrs.get(ldapAttrEmail) != null && attrs.get(ldapAttrEmail).get() != null) {
                                details.put("email", attrs.get(ldapAttrEmail).get().toString());
                            }
                            if (attrs.get(ldapAttrFirstName) != null && attrs.get(ldapAttrFirstName).get() != null) {
                                details.put("firstName", attrs.get(ldapAttrFirstName).get().toString());
                            }
                            if (attrs.get(ldapAttrLastName) != null && attrs.get(ldapAttrLastName).get() != null) {
                                details.put("lastName", attrs.get(ldapAttrLastName).get().toString());
                            }
                        } catch (Exception ignored) {
                            // ignore attribute errors
                        }

                        return details;
                    }
            );

            if (results != null && !results.isEmpty() && results.get(0) != null) {
                Map<String, String> ldapData = results.get(0);

                if (ldapData.get("email") != null && !ldapData.get("email").isBlank()) {
                    userDetails.put("email", ldapData.get("email"));
                }

                if (ldapData.get("firstName") != null && !ldapData.get("firstName").isBlank()) {
                    userDetails.put("firstName", ldapData.get("firstName"));
                }

                if (ldapData.get("lastName") != null && !ldapData.get("lastName").isBlank()) {
                    userDetails.put("lastName", ldapData.get("lastName"));
                }

                userDetails.put("source", "ldap");
            }

        } catch (Throwable ignored) {
            // NEVER BREAK LOGIN FLOW
            userDetails.put("source", "fallback");
        }

        return userDetails;
    }

    public User findUserByLdapId(String ldapId) {
        return userRepository.findBySsoProviderIdAndSsoProvider(ldapId, "LDAP").orElse(null);
    }

    private List<String> getLdapUserOu(String username) {
        try {
            if (ldapTemplate == null || usernameAttr == null || objectClassAttr == null) {
                return null;
            }

            AndFilter filter = new AndFilter();
            filter.and(new EqualsFilter("objectClass", objectClassAttr));
            filter.and(new EqualsFilter(usernameAttr, username));

            List<String> dns = ldapTemplate.search(
                    "", filter.encode(),
                    (ContextMapper<String>) ctx -> {
                        DirContextAdapter adapter = (DirContextAdapter) ctx;
                        return adapter.getDn().toString();
                    }
            );

            if (dns != null && !dns.isEmpty()) {
                return extractOuFromDn(dns.get(0));
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private List<String> extractOuFromDn(String dn) {
        List<String> ous = new ArrayList<>();
        if (dn == null || dn.isBlank()) {
            return ous;
        }
        String[] parts = dn.split(",");
        for (String part : parts) {
            if (part.toLowerCase().startsWith("ou=")) {
                ous.add(part.substring(3).trim());
            }
        }
        return ous;
    }

    private Role getRoleForOu(List<String> ous) {
        if (ldapOuRoleMappings == null || ldapOuRoleMappings.isBlank() || ous == null || ous.isEmpty()) {
            return getDefaultRoleForLdapUser();
        }
        Map<String, String> mappings = parseOuRoleMappings();
        for (String ou : ous) {
            String roleCode = mappings.get(ou.toLowerCase());
            if (roleCode != null && !roleCode.isBlank()) {
                return roleService.findDefaultRoles().stream()
                        .filter(role -> role.getCode().name().equalsIgnoreCase(roleCode))
                        .findFirst()
                        .orElse(getDefaultRoleForLdapUser());
            }
        }
        return getDefaultRoleForLdapUser();
    }

    private Map<String, String> parseOuRoleMappings() {
        Map<String, String> mappings = new HashMap<>();
        if (ldapOuRoleMappings == null || ldapOuRoleMappings.isBlank()) {
            return mappings;
        }
        String[] pairs = ldapOuRoleMappings.split("[|]");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                mappings.put(kv[0].trim().toLowerCase(), kv[1].trim());
            }
        }
        return mappings;
    }
}