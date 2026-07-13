package com.grash.dto.analytics.parts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartStats {
    private double totalConsumptionCost;
    private int consumedCount;
}
