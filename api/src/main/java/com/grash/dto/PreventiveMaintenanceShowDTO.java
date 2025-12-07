package com.grash.dto;

import com.grash.model.Schedule;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class PreventiveMaintenanceShowDTO extends WorkOrderBaseShowDTO {

    private String name;

    private Schedule schedule;

    private String customId;

    private Date nextWorkOrderDate;
}
