package com.grash.dto;

import com.grash.model.Schedule;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PreventiveMaintenanceShowDTO extends WorkOrderBaseShowDTO {

    private String name;

    private Schedule schedule;

    private String customId;
}
