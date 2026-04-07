package com.grash.model;

import com.grash.model.abstracts.Audit;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

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
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    private Role role;

    public UserInvitation(String email, Role role) {
        this.email = email;
        this.role = role;
    }
}


