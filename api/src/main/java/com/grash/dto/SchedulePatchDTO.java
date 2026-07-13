package com.grash.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class SchedulePatchDTO {
    private Date startsOn;

    private int frequency;

    private Date endsOn;

    private Integer dueDateDelay;
}
