package com.grash.service;

import com.grash.dto.DateRange;
import com.grash.model.Company;
import com.grash.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class AnalyticsCacheServiceTest {

    private AnalyticsCacheService analyticsCacheService;
    private User userOfCompany1;
    private User userOfCompany2;
    private DateRange dateRange;

    @BeforeEach
    void setUp() {
        analyticsCacheService = new AnalyticsCacheService();
        userOfCompany1 = user(10L, 1L);
        userOfCompany2 = user(20L, 2L);
        dateRange = DateRange.builder().start(new Date(0)).end(new Date(86_400_000L)).build();
    }

    @Test
    void key_withoutCompanyId_usesTheUsersCompany() {
        assertTrue(analyticsCacheService.key(userOfCompany1, null, dateRange).startsWith("c1_"));
    }

    @Test
    void key_withCompanyId_usesTheRequestedCompany() {
        assertTrue(analyticsCacheService.key(userOfCompany1, 3L, dateRange).startsWith("c3_"));
    }

    @Test
    void key_isStableUntilInvalidated() {
        assertEquals(analyticsCacheService.key(userOfCompany1, null, dateRange),
                analyticsCacheService.key(userOfCompany1, null, dateRange));
    }

    @Test
    void invalidateCompany_changesOnlyThatCompanysKeys() {
        String company1Before = analyticsCacheService.key(userOfCompany1, null, dateRange);
        String company2Before = analyticsCacheService.key(userOfCompany2, null, dateRange);
        String company1WithoutRangeBefore = analyticsCacheService.key(userOfCompany1, null);

        analyticsCacheService.invalidateCompany(1L);

        assertNotEquals(company1Before, analyticsCacheService.key(userOfCompany1, null, dateRange));
        assertNotEquals(company1WithoutRangeBefore, analyticsCacheService.key(userOfCompany1, null));
        assertEquals(company2Before, analyticsCacheService.key(userOfCompany2, null, dateRange));
    }

    @Test
    void invalidateCompany_alsoAffectsSuperAccountsViewingThatCompany() {
        String superAccountView = analyticsCacheService.key(userOfCompany2, 1L, dateRange);

        analyticsCacheService.invalidateCompany(1L);

        assertNotEquals(superAccountView, analyticsCacheService.key(userOfCompany2, 1L, dateRange));
    }

    @Test
    void invalidateAll_changesEveryCompanysKeys() {
        String company1Before = analyticsCacheService.key(userOfCompany1, null, dateRange);
        String company2Before = analyticsCacheService.key(userOfCompany2, null, dateRange);

        analyticsCacheService.invalidateAll();

        assertNotEquals(company1Before, analyticsCacheService.key(userOfCompany1, null, dateRange));
        assertNotEquals(company2Before, analyticsCacheService.key(userOfCompany2, null, dateRange));
    }

    @Test
    void invalidateCompany_withNullCompany_invalidatesEverything() {
        String company2Before = analyticsCacheService.key(userOfCompany2, null, dateRange);

        analyticsCacheService.invalidateCompany(null);

        assertNotEquals(company2Before, analyticsCacheService.key(userOfCompany2, null, dateRange));
    }

    private static User user(Long userId, Long companyId) {
        Company company = new Company();
        company.setId(companyId);
        User user = new User();
        user.setId(userId);
        user.setCompany(company);
        return user;
    }
}
