package com.grash.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Date;

@Data
@AllArgsConstructor
public class InvitationPreviewDTO {
    private String email;
    private Long roleId;
    private String roleName;
    private Long companyId;
    private String companyName;
    private Date expiresAt;
}
