package com.grash.controller;

import com.grash.dto.*;
import com.grash.model.*;
import com.grash.service.*;
import com.grash.security.JwtTokenProvider;
import com.grash.model.Role;
import com.grash.model.OwnUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/superadmin")
@PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
public class SuperAdminController {

    @Autowired
    private CompanyService companyService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * List all companies excluding the superadmin's own company.
     */
    @GetMapping("/companies")
    public ResponseEntity<List<SuperAdminCompanyDTO>> getAllCompanies(@AuthenticationPrincipal OwnUser currentUser) {
        // Fetch all companies
        List<Company> all = companyService.getAll();
        // Filter out the superadmin's own company
        Long ownCompanyId = currentUser.getCompany() != null ? currentUser.getCompany().getId() : null;
        List<SuperAdminCompanyDTO> result = all.stream()
                .filter(c -> !c.getId().equals(ownCompanyId))
                .map(this::toCompanyDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    private SuperAdminCompanyDTO toCompanyDTO(Company c) {
        SuperAdminCompanyDTO dto = new SuperAdminCompanyDTO();
        dto.setId(c.getId());
        dto.setName(c.getName());
        dto.setEmail(c.getEmail());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setSubscriptionPlanName(c.getSubscriptionPlan() != null ? c.getSubscriptionPlan().getName() : null);
        List<User> users = userRepository.findByCompany_Id(c.getId());
        dto.setUserCount(users.size());
        return dto;
    }

    @GetMapping("/companies/{id}")
    public ResponseEntity<SuperAdminCompanyDetailDTO> getCompanyDetail(@PathVariable Long id) {
        Company company = companyService.findById(id);
        if (company == null) {
            return ResponseEntity.notFound().build();
        }
        SuperAdminCompanyDetailDTO dto = new SuperAdminCompanyDetailDTO();
        dto.setId(company.getId());
        dto.setName(company.getName());
        dto.setEmail(company.getEmail());
        dto.setCreatedAt(company.getCreatedAt());
        dto.setSubscriptionPlanName(company.getSubscriptionPlan() != null ? company.getSubscriptionPlan().getName() : null);
        List<User> users = userRepository.findByCompany_Id(id);
        List<UserResponseDTO> userDtos = users.stream()
                .map(u -> UserResponseDTO dto = new UserResponseDTO();
        dto.setId(u.getId());
        dto.setUsername(u.getUsername());
        dto.setEmail(u.getEmail());
        dto.setRole(u.getRole());
        return dto;)
                .collect(Collectors.toList());
        dto.setUsers(userDtos);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/companies/{companyId}/users/invite")
    public ResponseEntity<Void> inviteUser(@PathVariable Long companyId, @RequestBody SuperAdminInviteUserDTO inviteDto) {
        Role role = roleService.findById(inviteDto.getRoleId());
        if (role == null) {
            return ResponseEntity.badRequest().build();
        }
        // Find company owner
        List<User> owners = userRepository.findByCompany_Id(companyId).stream()
                .filter(u -> u.isOwnsCompany())
                .collect(Collectors.toList());
        if (owners.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        User inviter = owners.get(0);
        userService.invite(inviteDto.getEmail(), role, inviter, false);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/switch/{userId}")
    public ResponseEntity<AuthResponse> switchToUser(@PathVariable Long userId) {
        User target = userService.findById(userId);
        if (target == null) {
            return ResponseEntity.notFound().build();
        }
        String token = jwtTokenProvider.createToken(target.getEmail(), List.of(target.getRole().getRoleType()));
        AuthResponse resp = new AuthResponse(token, target.getEmail());
        return ResponseEntity.ok(resp);
    }
}
