package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInvitationMiniDTO {
    @Schema(description = "Email address of the invited user")
    private String email;

    @Schema(description = "Role ID assigned to the invitation")
    private Long roleId;

    @Schema(description = "Role name assigned to the invitation")
    private String roleName;
}
