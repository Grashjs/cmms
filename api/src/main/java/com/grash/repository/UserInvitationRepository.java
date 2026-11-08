package com.grash.repository;

import com.grash.model.UserInvitation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface UserInvitationRepository extends JpaRepository<UserInvitation, Long> {
    Optional<UserInvitation> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM UserInvitation i WHERE i.tokenHash = :tokenHash")
    Optional<UserInvitation> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE UserInvitation i SET i.revokedAt = :now WHERE i.company.id = :companyId " +
            "AND lower(i.email) = lower(:email) AND i.consumedAt IS NULL AND i.revokedAt IS NULL")
    int revokePending(@Param("companyId") Long companyId, @Param("email") String email, @Param("now") Date now);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE UserInvitation i SET i.revokedAt = :now WHERE i.invitedById = :userId " +
            "AND i.consumedAt IS NULL AND i.revokedAt IS NULL")
    int revokeByIssuer(@Param("userId") Long userId, @Param("now") Date now);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE UserInvitation i SET i.revokedAt = :now WHERE i.role.id = :roleId " +
            "AND i.consumedAt IS NULL AND i.revokedAt IS NULL")
    int revokeByRole(@Param("roleId") Long roleId, @Param("now") Date now);

    @Query("SELECT i FROM UserInvitation i WHERE i.company.id = :companyId " +
            "AND i.createdAt >= :since AND i.expiresAt > :now AND i.tokenHash IS NOT NULL " +
            "AND i.consumedAt IS NULL AND i.revokedAt IS NULL AND i.role IS NOT NULL " +
            "AND NOT EXISTS (SELECT u FROM User u WHERE lower(u.email) = lower(i.email))")
    List<UserInvitation> findPendingByCompanyAndCreatedAtAfter(@Param("companyId") Long companyId,
                                                              @Param("since") Date since,
                                                              @Param("now") Date now);
}
