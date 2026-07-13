package com.grash.dto;

import com.grash.model.OwnUser;
import com.grash.model.TimeCategory;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class LaborPatchDTO {
    private OwnUser assignedTo;

    private boolean includeToTotalTime;

    private long hourlyRate;

    private int duration;

    private Date startedAt;
    private TimeCategory timeCategory;
}
