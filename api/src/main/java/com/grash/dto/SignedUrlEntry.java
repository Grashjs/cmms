package com.grash.dto;

import java.time.Instant;

public record SignedUrlEntry(String url, Instant expiresAt) {
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}