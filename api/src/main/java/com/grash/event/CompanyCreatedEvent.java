package com.grash.event;

import com.grash.model.OwnUser;
import lombok.Data;
import lombok.Getter;

@Getter
@Data
public class CompanyCreatedEvent {
    private final OwnUser user;

    public CompanyCreatedEvent(OwnUser user) {
        this.user = user;
    }

}