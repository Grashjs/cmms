package com.grash.dto;

import com.grash.model.Role;
import com.grash.model.enums.Language;
import io.swagger.annotations.ApiModelProperty;
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

    private String subscriptionPlanId;

    private Boolean demo;

    private UtmParams utmParams;

}

