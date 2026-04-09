package com.grash.event;

import com.grash.model.User;
import lombok.Data;
import lombok.Getter;

@Getter
@Data
public class CompanyCreatedEvent {
    private final User user;

    public CompanyCreatedEvent(User user) {
        this.user = user;
    }

}