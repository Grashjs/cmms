package com.grash.service;

import com.grash.dto.InvitationPreviewDTO;
import com.grash.dto.IssuedInvitationDTO;
import com.grash.dto.UserInvitationMiniDTO;
import com.grash.exception.CustomException;
import com.grash.model.Role;
import com.grash.model.User;
import com.grash.model.UserInvitation;
import com.grash.repository.UserInvitationRepository;
import com.grash.repository.UserRepository;
import jakarta.persistence.EntityManager;
import com.grash.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserInvitationService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long VALIDITY_MILLIS = Duration.ofDays(7).toMillis();
    private final UserInvitationRepository userInvitationRepository;
    private final CompanyAuthorizationService authorization;
    private final UserRepository users;
    private final EntityManager em;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Transactional
    public IssuedInvitationDTO issue(String email, Role role, User inviter) {
        inviter = authorization.actor(inviter);
        role = authorization.role(role);
        authorization.assertCanInvite(inviter, role);
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (!Helper.isValidEmailAddress(normalizedEmail)) {
            throw new CustomException("Invalid invitation email", HttpStatus.NOT_ACCEPTABLE);
        }
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + VALIDITY_MILLIS);
        userInvitationRepository.revokePending(inviter.getCompany().getId(), normalizedEmail, now);
        UserInvitation invitation = UserInvitation.builder().email(normalizedEmail).role(role)
                .company(inviter.getCompany()).tokenHash(hash(token)).expiresAt(expiresAt)
                .invitedById(inviter.getId()).invitedRoleId(role.getId())
                .roleAuthorizationHash(authorization.fingerprint(role))
                .issuerAuthorizationHash(authorization.fingerprint(inviter.getRole())).build();
        invitation.setCreatedBy(inviter.getId());
        userInvitationRepository.save(invitation);
        return new IssuedInvitationDTO(normalizedEmail,
                frontendUrl.replaceAll("/+$", "") + "/account/register#invitationToken=" + token, expiresAt);
    }

    // The lock must survive until the caller commits both account creation and consumption.
    @Transactional(propagation = Propagation.MANDATORY)
    public UserInvitation lockValidInvitation(String token) {
        String tokenHash = hash(token);
        UserInvitation scope = userInvitationRepository.findByTokenHash(tokenHash).orElseThrow(this::invalidInvitation);
        if (scope.getCompany() == null) throw invalidInvitation();
        authorization.lockCompany(scope.getCompany().getId());
        UserInvitation invitation = userInvitationRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(this::invalidInvitation);
        if (em.contains(invitation)) em.refresh(invitation);
        validate(invitation);
        return invitation;
    }

    @Transactional(readOnly = true)
    public InvitationPreviewDTO preview(String token) {
        UserInvitation invitation = userInvitationRepository.findByTokenHash(hash(token))
                .orElseThrow(this::invalidInvitation);
        validate(invitation);
        return new InvitationPreviewDTO(invitation.getEmail(), invitation.getRole().getId(),
                invitation.getRole().getName(), invitation.getCompany().getId(),
                invitation.getCompany().getName(), invitation.getExpiresAt());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void consume(UserInvitation invitation, User user) {
        validate(invitation);
        if (user == null || user.getId() == null || user.getRole() == null || user.getCompany() == null
                || !invitation.getEmail().equalsIgnoreCase(user.getEmail())
                || !Objects.equals(invitation.getRole().getId(), user.getRole().getId())
                || !Objects.equals(invitation.getCompany().getId(), user.getCompany().getId())) {
            throw invalidInvitation();
        }
        invitation.setConsumedAt(new Date());
        invitation.setAcceptedUserId(user.getId());
        userInvitationRepository.save(invitation);
    }

    @Transactional(readOnly = true)
    public Collection<UserInvitationMiniDTO> getDistinctByCompanyInLastWeek(Long companyId) {
        Date now = new Date();
        Date weekAgo = new Date(now.getTime() - VALIDITY_MILLIS);
        return userInvitationRepository.findPendingByCompanyAndCreatedAtAfter(companyId, weekAgo, now).stream()
                .collect(Collectors.toMap(inv -> inv.getEmail().toLowerCase(Locale.ROOT), Function.identity(),
                        (first, second) -> first.getCreatedAt().after(second.getCreatedAt()) ? first : second))
                .values().stream()
                .map(inv -> new UserInvitationMiniDTO(inv.getEmail(), inv.getRole().getId(), inv.getRole().getName()))
                .collect(Collectors.toList());
    }

    private void validate(UserInvitation invitation) {
        if (invitation == null || invitation.getTokenHash() == null || invitation.getCompany() == null
                || invitation.getCompany().getId() == null || invitation.getEmail() == null
                || invitation.getExpiresAt() == null || !invitation.getExpiresAt().after(new Date())
                || invitation.getConsumedAt() != null || invitation.getRevokedAt() != null
                || !authorization.roleInCompany(invitation.getRole(), invitation.getCompany())) {
            throw invalidInvitation();
        }
        try {
            if (invitation.getInvitedById() == null || invitation.getRoleAuthorizationHash() == null
                    || invitation.getIssuerAuthorizationHash() == null) throw invalidInvitation();
            User issuer = users.findById(invitation.getInvitedById()).orElseThrow(this::invalidInvitation);
            if (em.contains(issuer)) em.refresh(issuer);
            Role currentRole = authorization.role(invitation.getRole());
            Role issuerRole = authorization.role(issuer.getRole());
            issuer.setRole(issuerRole);
            if (issuer.getCompany() == null || !Objects.equals(issuer.getCompany().getId(), invitation.getCompany().getId())
                    || !Objects.equals(invitation.getInvitedRoleId(), currentRole.getId())
                    || !invitation.getRoleAuthorizationHash().equals(authorization.fingerprint(currentRole))
                    || !invitation.getIssuerAuthorizationHash().equals(authorization.fingerprint(issuerRole)))
                throw invalidInvitation();
            authorization.assertCanInvite(issuer, currentRole);
        } catch (CustomException exception) {
            throw invalidInvitation();
        }
    }

    private String hash(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) {
            throw invalidInvitation();
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm not available", exception);
        }
    }

    private CustomException invalidInvitation() {
        return new CustomException("Invitation is invalid, expired or already used. Request a new invitation.",
                HttpStatus.NOT_ACCEPTABLE);
    }
}
