package com.grash.dto;

import com.grash.model.Role;
import com.grash.model.enums.Language;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class UserSignupRequest {

    @ApiModelProperty(position = 1)
    @NotNull
    @Email
    private String email;
    @ApiModelProperty(position = 2)
    @NotNull
    private String password;

    @ApiModelProperty(position = 3)
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

}
