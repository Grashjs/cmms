package com.grash.dto.analytics.requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestStats {
    private int approved;
    private int pending;
    private int cancelled;
    private long cycleTime;
}
