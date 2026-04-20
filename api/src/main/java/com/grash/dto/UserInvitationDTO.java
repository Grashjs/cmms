package com.grash.dto;

import com.grash.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Collection;

@Data
@NoArgsConstructor
@Schema(description = "DTO for inviting users to the system")
public class UserInvitationDTO {
    @Schema(description = "Role to assign to invited users")
    @NotNull
    private Role role;

    @Schema(description = "List of email addresses to invite")
    private Collection<String> emails = new ArrayList<>();

    @Schema(description = "Whether to disable sending invitation emails")
    private Boolean disableSendingEmail;
}

