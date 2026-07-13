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
public class DowntimesMeantimeByDate {
    //hours
    private long meantime;
    private Date date;
}
