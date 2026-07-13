package com.grash.dto.analytics.workOrders;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import net.bytebuddy.implementation.bind.annotation.Super;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WOStatuses {
    private int open;
    private int onHold;
    private int inProgress;
    private int complete;
}
