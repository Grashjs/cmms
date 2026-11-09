package com.grash.configuration;

import com.grash.model.*;
import com.grash.model.enums.RoleType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MobileOfflinePropertiesTest {
    private User user() {
        Company company = new Company(); company.setId(52L);
        Role role = new Role(); role.setRoleType(RoleType.ROLE_CLIENT);
        User user = new User(); user.setId(34L); user.setCompany(company); user.setRole(role); user.setEnabled(true);
        return user;
    }

    @Test void supportsCompanyFirstAndCombinedRules() {
        for (String value : new String[]{"52:34", "52:*", "*:*", "51:*, 52:34"}) {
            var config = new MobileOfflineProperties(true, value); config.validate();
            assertTrue(config.isEligible(user()), value);
        }
        for (String value : new String[]{"", "34:52", "51:*", "52:35"}) {
            var config = new MobileOfflineProperties(true, value); config.validate();
            assertFalse(config.isEligible(user()), value);
        }
    }

    @Test void disabledAndDisabledAccountFailClosed() {
        var disabled = new MobileOfflineProperties(false, "*:*"); disabled.validate();
        assertFalse(disabled.isEligible(user()));
        var enabled = new MobileOfflineProperties(true, "*:*"); enabled.validate();
        User user = user(); user.setEnabled(false);
        assertFalse(enabled.isEligible(user));
    }

    @Test void malformedAllowlistFailsStartupRatherThanGrantingAccess() {
        for (String value : new String[]{"*:34", "52", "52:*;", "52:34,", "0:34", "-1:*", "52:abc"}) {
            assertThrows(IllegalArgumentException.class, () -> new MobileOfflineProperties(true, value).validate(), value);
        }
    }
}
