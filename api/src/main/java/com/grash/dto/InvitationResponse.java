package com.grash.dto;

import lombok.Getter;
import java.util.List;

@Getter
public class InvitationResponse extends SuccessResponse {
    private final List<IssuedInvitationDTO> invitations;

    public InvitationResponse(boolean success, String message, List<IssuedInvitationDTO> invitations) {
        super(success, message);
        this.invitations = invitations;
    }
}
