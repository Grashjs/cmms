package com.grash.model.abstracts;

import lombok.Data;

import javax.persistence.MappedSuperclass;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Data
@MappedSuperclass
public abstract class Time extends CompanyAudit {

    private long duration = 0; //seconds

    public List<? extends Number> getFormattedDuration() {
        int day = (int) TimeUnit.SECONDS.toDays(duration);
        long hours = TimeUnit.SECONDS.toHours(duration) - (day * 24);
        long minute = TimeUnit.SECONDS.toMinutes(duration) - (TimeUnit.SECONDS.toHours(duration) * 60);
        long second = TimeUnit.SECONDS.toSeconds(duration) - (TimeUnit.SECONDS.toMinutes(duration) * 60);
        return Arrays.asList(day, hours, minute, second);
    }
}
