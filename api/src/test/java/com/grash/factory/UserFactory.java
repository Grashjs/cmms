package com.grash.factory;

import com.grash.model.Role;
import com.grash.model.User;
import com.grash.model.UserSettings;

public final class UserFactory {

    private UserFactory() {
    }

    public static User createUser(String email, Role role) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setUsername(email);
        user.setPassword("hashed-password");
        user.setRole(role);
        user.setEnabled(true);
        user.setEnabledInSubscription(true);
        user.setUserSettings(new UserSettings());
        return user;
    }

    public static User createAdminUser() {
        return createUser("admin@test.com", RoleFactory.createAdminRole());
    }
}
