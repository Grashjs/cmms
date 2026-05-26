package com.grash.repository;

import com.grash.model.UserInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface UserInvitationRepository extends JpaRepository<UserInvitation, Long> {
    List<UserInvitation> findByRole_IdAndEmailIgnoreCase(Long id, String email);

    @Query("SELECT u FROM UserInvitation u WHERE u.createdBy IN " +
            "(SELECT us.id FROM User us WHERE us.company.id = :companyId) " +
            "AND u.createdAt >= :since " +
            "AND NOT EXISTS (SELECT u2 FROM User u2 WHERE lower(u2.email) = lower(u.email))")
    List<UserInvitation> findPendingByCompanyAndCreatedAtAfter(@Param("companyId") Long companyId,
                                                               @Param("since") Date since);
}

