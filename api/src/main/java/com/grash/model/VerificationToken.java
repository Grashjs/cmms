package com.grash.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.persistence.*;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

@Entity
@Data
@Schema(description = "Verification token for email verification and authentication")
public class VerificationToken {

    private static final int EXPIRATION = 20; //minutes

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private long id;

    @Schema(description = "Token string")
    private String token;

    @Schema(description = "Payload data associated with the token")
    private String payload;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Schema(description = "Creation date", accessMode = Schema.AccessMode.READ_ONLY)
    private Date createdAt;

    @Schema(description = "Token expiry date")
    private Date expiryDate;

    public VerificationToken() {
        super();
    }

    public VerificationToken(final String token, final User user, final String payload) {
        super();
        Calendar calendar = Calendar.getInstance();

        this.token = token;
        this.user = user;
        this.createdAt = new Date(calendar.getTime().getTime());
        this.payload = payload;
        this.expiryDate = calculateExpiryDate();
    }

    private Date calculateExpiryDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Timestamp(calendar.getTime().getTime()));
        calendar.add(Calendar.MINUTE, VerificationToken.EXPIRATION);
        return new Date(calendar.getTime().getTime());
    }
}

