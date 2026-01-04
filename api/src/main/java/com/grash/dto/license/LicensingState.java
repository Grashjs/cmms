package com.grash.dto.license;

import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
public class LicensingState {
    private boolean hasLicense;
    private boolean valid;
    private String planName;
    @Builder.Default
    private Set<String> entitlements = new HashSet<>();
    private Date expirationDate;

    private int usersCount;
}
