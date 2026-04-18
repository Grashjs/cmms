package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LdapLoginRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "Username is required")
    @Schema(description = "LDAP username", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotNull(message = "Password is required")
    @Schema(description = "LDAP password", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}

