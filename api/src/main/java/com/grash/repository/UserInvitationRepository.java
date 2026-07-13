package com.grash.repository;

import com.grash.model.UserInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface UserInvitationRepository extends JpaRepository<UserInvitation, Long> {
    Collection<UserInvitation> findByRole_IdAndEmailIgnoreCase(Long id, String email);
}

