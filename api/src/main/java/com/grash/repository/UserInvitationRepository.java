package com.grash.repository;

import com.grash.model.UserInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface UserInvitationRepository extends JpaRepository<UserInvitation, Long> {
    List<UserInvitation> findByRole_IdAndEmailIgnoreCase(Long id, String email);
}

