package com.grash.service;

import com.grash.model.UserInvitation;
import com.grash.repository.UserInvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

}
