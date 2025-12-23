package com.grash.dto.license;

import lombok.Builder;
import lombok.Data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
public class LicensingState {
    private boolean valid;
    @Builder.Default
    private Set<String> entitlements = new HashSet<>();
}
