package com.grash.dto.analytics.assets;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DowntimesByDate {
    //seconds
    private long duration;
    private double workOrdersCosts;
    private Date date;
}
