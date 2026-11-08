package com.grash.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import java.util.Date;

@Data
@AllArgsConstructor
public class IssuedInvitationDTO {
    private String email;
    @ToString.Exclude
    private String invitationLink;
    private Date expiresAt;
}
