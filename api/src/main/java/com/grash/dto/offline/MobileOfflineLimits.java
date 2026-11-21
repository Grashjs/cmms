package com.grash.dto.offline;

/** Fixed pilot limits, shared by the eligibility response and server guards. */
public record MobileOfflineLimits(int preparedWorkOrders, int tasks, int comments, int history,
                                  int snapshotItems, int snapshotBytes, int receiptIds) {
    public static final MobileOfflineLimits PILOT = new MobileOfflineLimits(25, 500, 500, 500,
            2000, 2 * 1024 * 1024, 100);
}
