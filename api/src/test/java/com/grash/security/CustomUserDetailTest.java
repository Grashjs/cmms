package com.grash.security;

import com.grash.model.Role;
import com.grash.model.User;
import com.grash.model.enums.RoleType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CustomUserDetailTest {

    private User createUserWithRole(boolean enabled) {
        Role role = new Role();
        role.setRoleType(RoleType.ROLE_CLIENT);
        User user = new User();
        user.setEmail("test@test.com");
        user.setPassword("secret");
        user.setRole(role);
        user.setEnabled(enabled);
        return user;
    }

    @Test
    void getAuthorities_returnsRoleTypeAuthority() {
        CustomUserDetail detail = CustomUserDetail.builder().user(createUserWithRole(true)).build();

        assertEquals(1, detail.getAuthorities().size());
        assertEquals("ROLE_CLIENT", detail.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void getPassword_returnsUserPassword() {
        CustomUserDetail detail = CustomUserDetail.builder().user(createUserWithRole(true)).build();

        assertEquals("secret", detail.getPassword());
    }

    @Test
    void getUsername_returnsUserEmail() {
        CustomUserDetail detail = CustomUserDetail.builder().user(createUserWithRole(true)).build();

        assertEquals("test@test.com", detail.getUsername());
    }

    @Test
    void enabledUser_allAccountStatusMethodsReturnTrue() {
        CustomUserDetail detail = CustomUserDetail.builder().user(createUserWithRole(true)).build();

        assertTrue(detail.isAccountNonExpired());
        assertTrue(detail.isAccountNonLocked());
        assertTrue(detail.isCredentialsNonExpired());
        assertTrue(detail.isEnabled());
    }

    @Test
    void disabledUser_allAccountStatusMethodsReturnFalse() {
        CustomUserDetail detail = CustomUserDetail.builder().user(createUserWithRole(false)).build();

        assertFalse(detail.isAccountNonExpired());
        assertFalse(detail.isAccountNonLocked());
        assertFalse(detail.isCredentialsNonExpired());
        assertFalse(detail.isEnabled());
    }

    @Test
    void getUser_returnsWrappedUser() {
        User user = createUserWithRole(true);
        CustomUserDetail detail = CustomUserDetail.builder().user(user).build();

        assertSame(user, detail.getUser());
    }

    @Test
    void setUser_updatesWrappedUser() {
        CustomUserDetail detail = CustomUserDetail.builder().user(createUserWithRole(true)).build();
        User newUser = createUserWithRole(false);

        detail.setUser(newUser);

        assertSame(newUser, detail.getUser());
        assertFalse(detail.isEnabled());
    }
}
