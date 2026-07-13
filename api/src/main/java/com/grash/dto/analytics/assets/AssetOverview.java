package com.grash.dto.analytics.assets;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetOverview {
    private long mtbf;
    private long mttr;
    private long downtime;
    private long uptime;
    private double totalCost;
}
