package com.grash.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class WorkOrderMessageReactionDTO {

    private String reaction;

    private int count;

    private List<UserMiniDTO> users;

    private boolean currentUserReacted;

    public WorkOrderMessageReactionDTO(String reaction, int count, List<UserMiniDTO> users, boolean currentUserReacted) {
        this.reaction = reaction;
        this.count = count;
        this.users = users;
        this.currentUserReacted = currentUserReacted;
    }
}
