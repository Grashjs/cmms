package com.grash.automation.event;

import com.grash.security.CustomUserDetail;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Who caused the change being published, if anybody.
 *
 * <p>Read at publish time, on the request thread, because it cannot be read later: the listener
 * runs on an executor where the security context is empty. Null is an ordinary answer — a change
 * from the request portal, a scheduled job or another rule has no logged-in actor. Same reading
 * as {@code AuditConfig}'s auditor provider, kept here so the services that publish do not have
 * to know about security plumbing.
 */
public final class CurrentActor {

    private CurrentActor() {
    }

    public static Long userIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof CustomUserDetail detail)) {
            return null;
        }
        return detail.getUser() == null ? null : detail.getUser().getId();
    }
}
