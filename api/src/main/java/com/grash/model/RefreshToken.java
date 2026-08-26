package com.grash.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Opaque refresh token used to renew short-lived access tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Column(nullable = false, unique = true)
    @Schema(description = "SHA-256 hash of the raw refresh token", accessMode = Schema.AccessMode.READ_ONLY)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    @Schema(description = "User the refresh token belongs to")
    private User user;

    @Schema(description = "Token expiry date")
    private Date expiresAt;

    @Schema(description = "Creation date", accessMode = Schema.AccessMode.READ_ONLY)
    private Date createdAt;

    @Schema(description = "Whether the token has been revoked or consumed by rotation", accessMode = Schema.AccessMode.READ_ONLY)
    private boolean revoked;

    @Schema(description = "Hash of the token that replaced this one after rotation", accessMode = Schema.AccessMode.READ_ONLY)
    private String replacedByTokenHash;
}
