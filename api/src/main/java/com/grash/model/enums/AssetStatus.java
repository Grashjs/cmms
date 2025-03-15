package com.grash.model.enums;

import lombok.Getter;
import org.springframework.context.MessageSource;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum AssetStatus {
    OPERATIONAL(RealAssetStatus.UP),
    DOWN(RealAssetStatus.DOWN),
    MODERNIZATION(RealAssetStatus.UP),
    STANDBY(RealAssetStatus.UP),
    INSPECTION_SCHEDULED(RealAssetStatus.UP),
    COMMISSIONING(RealAssetStatus.UP),
    EMERGENCY_SHUTDOWN(RealAssetStatus.DOWN);

    private final RealAssetStatus realAssetStatus;

    AssetStatus(RealAssetStatus realAssetStatus) {
        this.realAssetStatus = realAssetStatus;
    }

    public static AssetStatus getAssetStatusFromString(String string, Locale locale, MessageSource messageSource) {
        if (string == null) {
            return null;
        }
        return Arrays.stream(AssetStatus.values()).filter(assetStatus -> messageSource.getMessage(assetStatus.name(),
                null, locale).equalsIgnoreCase(string.trim())).findFirst().orElse(null);
    }

    private enum RealAssetStatus {
        UP,
        DOWN;
    }

    public boolean isReallyDown() {
        return this.realAssetStatus == RealAssetStatus.DOWN;
    }
}
