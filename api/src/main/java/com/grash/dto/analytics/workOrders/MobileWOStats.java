package com.grash.dto.analytics.workOrders;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MobileWOStats {
    private int open;
    private int onHold;
    private int inProgress;
    private int complete;
    private int today;
    private int high;
}
