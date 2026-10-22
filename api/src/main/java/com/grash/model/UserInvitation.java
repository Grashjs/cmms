package com.grash.model;

import com.grash.model.abstracts.Audit;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
@Schema(description = "User invitation for inviting new users by email")
public class UserInvitation extends Audit {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotNull
    @Schema(description = "Email address of the invited user", requiredMode = Schema.RequiredMode.REQUIRED)
    @Column(updatable = false)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(updatable = false)
    @ToString.Exclude
    private Role role;

    // Nullable only for revoked legacy invitations whose tenant cannot be recovered.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(updatable = false)
    @ToString.Exclude
    private Company company;

    @JsonIgnore
    @ToString.Exclude
    @Column(length = 64, unique = true, updatable = false)
    private String tokenHash;

    @Column(updatable = false)
    private Date expiresAt;

    private Date consumedAt;

    private Date revokedAt;

    // Scalar IDs preserve issuance/acceptance evidence if either user is later deleted.
    @Column(updatable = false)
    private Long invitedById;

    @Column(updatable = false)
    private Long invitedRoleId;

    @JsonIgnore
    @Column(length = 64, updatable = false)
    private String roleAuthorizationHash;

    @JsonIgnore
    @Column(length = 64, updatable = false)
    private String issuerAuthorizationHash;

    private Long acceptedUserId;

    public UserInvitation(String email, Role role) {
        this.email = email;
        this.role = role;
    }
}


