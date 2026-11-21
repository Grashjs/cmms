package com.grash.dto.offline;

public record MobileOfflineTimePermissions(boolean createTimer, boolean resumeTimer,
                                           boolean stopTimer, boolean createManualTime) {}
