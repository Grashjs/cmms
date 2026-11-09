package com.grash.dto.offline;

import java.time.Instant;

public record MobileOfflineEligibility(boolean enabled, boolean eligible, Long companyId, Long userId,
                                       Instant serverTime, Instant authorizedUntil, MobileOfflineLimits limits) {}
