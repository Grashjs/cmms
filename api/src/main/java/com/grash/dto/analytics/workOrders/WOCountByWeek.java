package com.grash.dto.analytics.workOrders;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WOCountByWeek {
    private int count;
    private int compliant;
    private int reactive;
    private Date date;
}
