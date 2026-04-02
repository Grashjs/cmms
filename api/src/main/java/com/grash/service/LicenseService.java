package com.grash.service;

import com.grash.dto.license.LicenseEntitlement;
import com.grash.dto.license.LicensingState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LicenseService {

    public LicensingState getLicensingState() {
        return LicensingState.builder()
                .hasLicense(true)
                .valid(true)
                .build();
    }

    public boolean isSSOEnabled() {
        return true;
    }

    public boolean hasEntitlement(LicenseEntitlement entitlement) {
        return true;
    }
}
