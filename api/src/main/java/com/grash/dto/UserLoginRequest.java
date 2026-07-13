package com.grash.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
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
    private String type;
}
