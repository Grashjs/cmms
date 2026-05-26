package com.grash.service;

import com.grash.dto.UserInvitationMiniDTO;
import com.grash.model.UserInvitation;
import com.grash.repository.UserInvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserInvitationService {
    private final UserInvitationRepository userInvitationRepository;

    public UserInvitation create(UserInvitation UserInvitation) {
        return userInvitationRepository.save(UserInvitation);
    }

    public Collection<UserInvitation> getAll() {
        return userInvitationRepository.findAll();
    }

    public void delete(Long id) {
        userInvitationRepository.deleteById(id);
    }

    public Optional<UserInvitation> findById(Long id) {
        return userInvitationRepository.findById(id);
    }

    public List<UserInvitation> findByRoleAndEmail(Long id, String email) {
        return userInvitationRepository.findByRole_IdAndEmailIgnoreCase(id, email);
    }

    public Collection<UserInvitationMiniDTO> getDistinctByCompanyInLastWeek(Long companyId) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -7);
        Date weekAgo = cal.getTime();

        List<UserInvitation> invitations = userInvitationRepository.findPendingByCompanyAndCreatedAtAfter(companyId,
                weekAgo);

        return invitations.stream()
                .collect(Collectors.toMap(
                        inv -> inv.getEmail().toLowerCase(),
                        Function.identity(),
                        (inv1, inv2) -> inv1.getCreatedAt().after(inv2.getCreatedAt()) ? inv1 : inv2
                ))
                .values().stream()
                .map(inv -> new UserInvitationMiniDTO(inv.getEmail(), inv.getRole().getId(), inv.getRole().getName()))
                .collect(Collectors.toList());
    }

}
