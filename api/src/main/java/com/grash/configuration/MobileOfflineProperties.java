package com.grash.configuration;

import com.grash.model.User;
import com.grash.model.enums.RoleType;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class MobileOfflineProperties {
    private static final Pattern ACCOUNT = Pattern.compile("(?:[1-9][0-9]*:(?:[1-9][0-9]*|\\*)|\\*:\\*)");
    @Getter
    private final boolean enabled;
    private final String allowedAccounts;
    private List<String> rules = List.of();

    public MobileOfflineProperties(@Value("${mobile-offline.enabled:false}") boolean enabled,
                                   @Value("${mobile-offline.allowed-accounts:}") String allowedAccounts) {
        this.enabled = enabled;
        this.allowedAccounts = allowedAccounts;
    }

    @PostConstruct
    public void validate() {
        if (allowedAccounts.isBlank()) {
            rules = List.of();
            return;
        }
        rules = Arrays.stream(allowedAccounts.split(",", -1)).map(String::trim).toList();
        if (rules.stream().anyMatch(rule -> !ACCOUNT.matcher(rule).matches())) {
            throw new IllegalArgumentException("MOBILE_OFFLINE_ALLOWED_ACCOUNTS must contain comma-separated company:user, company:* or *:* entries");
        }
    }

    public boolean isEligible(User user) {
        if (!enabled || !user.isEnabled() || user.getCompany() == null || user.getRole() == null
                || user.getRole().getRoleType() != RoleType.ROLE_CLIENT) return false;
        String company = user.getCompany().getId().toString();
        return rules.contains("*:*") || rules.contains(company + ":*")
                || rules.contains(company + ":" + user.getId());
    }
}
