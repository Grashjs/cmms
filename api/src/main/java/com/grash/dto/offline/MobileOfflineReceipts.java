package com.grash.dto.offline;

import java.util.List;
import java.util.UUID;

public record MobileOfflineReceipts(List<MobileOfflineResult> results, List<UUID> missingOperationIds) {}
