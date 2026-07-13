package com.grash.dto;

import com.grash.model.Role;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;

@Data
@NoArgsConstructor
public class UserInvitationDTO {
    @NotNull
    private Role role;

    private Collection<String> emails = new ArrayList<>();
}
