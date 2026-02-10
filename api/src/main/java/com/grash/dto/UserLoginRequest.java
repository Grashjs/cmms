package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class UserLoginRequest implements Serializable {
    private static final long serialVersionUID = 5926468583005150707L;

    @NotNull
    private String email;
    @NotNull
    private String password;
    @NotNull
    @Schema(hidden = true)
    private String type = "CLIENT";
}

