package com.grash.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.grash.model.Role;
import com.grash.model.enums.Language;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
public class UserSignupRequest {

    @NotNull
    private String email;
    @NotNull
    private String password;

    @Schema(implementation = IdDTO.class)
    private Role role;

    @NotNull
    private String firstName;

    @NotNull
    private String lastName;

    @NotNull
    private String phone;

    private String companyName;

    private int employeesCount;

    private Language language;
    @Schema(hidden = true)
    private String subscriptionPlanId;
    @Schema(hidden = true)
    private Boolean demo;

    @Schema(hidden = true)
    private UtmParams utmParams;

    @JsonIgnore
    private Boolean skipMailSending;

    private String timeZone;

}

