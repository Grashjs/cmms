package com.grash.dto.analytics.workOrders;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WOStats {
    private int total;
    private int complete;
    private int compliant;
    private long avgCycleTime;
    private long mtta;
}
