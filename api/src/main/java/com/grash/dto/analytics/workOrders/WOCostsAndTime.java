package com.grash.dto.analytics.workOrders;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WOCostsAndTime {
    private double total;
    private double average;
    private double additionalCost;
    private double laborCost;
    private double partCost;
    private long laborTime;
}
